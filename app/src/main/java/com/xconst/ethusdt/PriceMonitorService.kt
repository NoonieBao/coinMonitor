package com.xconst.ethusdt

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.*
import androidx.core.app.NotificationCompat
import com.xconst.ethusdt.android.AlarmPlayer
import com.xconst.ethusdt.android.FlashlightController
import com.xconst.ethusdt.api.OkxHttpClient
import com.xconst.ethusdt.api.OkxWebSocketClient
import com.xconst.ethusdt.bus.AppBus
import com.xconst.ethusdt.bus.Direction
import com.xconst.ethusdt.bus.NetworkStatus
import com.xconst.ethusdt.bus.SocketStatus
import com.xconst.ethusdt.floatWindows.FloatWindowService
import com.xconst.ethusdt.store.AlarmConditions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

object Actions {
    const val START = "start"
    const val STOP_MONITOR = "stop_monitor"
    const val STOP_ALARM = "stop_alarm"
    const val KILL_MONITOR = "KILL_MONITOR"
    const val ACTION_EXIT_APP = "ACTION_EXIT_APP"
}

//object AlarmResult{
//
//    const val NETWORK = "start"
//
//    const val CONDITION = "CONDITION"
//
//}
class PriceMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "eth_monitor_v3"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var healthJob: Job? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var flashlight: FlashlightController
    private lateinit var networkAbilityMonitor: NetworkAbilityMonitor
    private lateinit var alarmConditions: AlarmConditions
    private lateinit var vibrator: Vibrator

    // ✅ 改为 nullable（核心修复）
    private var socketClient: OkxWebSocketClient? = null
    private var httpClient: OkxHttpClient? = null

    private var lastPriceMessageAt: Long = 0L
    private var networkLostSince: Long? = null
    private var lastNotifyTime = 0L



    override fun onCreate() {
        super.onCreate()

        createChannel()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        flashlight = FlashlightController(this)
        networkAbilityMonitor = NetworkAbilityMonitor(this)
        alarmConditions = AlarmConditions(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        observeNetwork()
        watchStreamHealth()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val stopFun = {
            AlarmPlayer.stop()
            stopVibration()
            flashlight.stopFlashing()
            AppBus.update { it.copy(isAlarming = false) }
            socketClient?.resetWebSocket()
        }

        when (intent?.action) {
            Actions.START -> {
                startMonitoring()
                stopFun()
            }
            Actions.STOP_MONITOR -> stopMonitoring()

            Actions.STOP_ALARM -> {
                stopFun()
            }

            Actions.KILL_MONITOR -> {
                stopMonitoring()
                sendBroadcast(Intent(Actions.ACTION_EXIT_APP))
            }


            else -> startMonitoring()
        }

        return START_STICKY
    }

    // =========================
    // 🚀 核心：启动逻辑
    // =========================
    private fun startMonitoring() {

        if (AppBus.appState.value.isMonitoring) return

        startForeground(1001, buildNotification("监控中"))

        AppBus.update { it.copy(isMonitoring = true) }

        lastPriceMessageAt = System.currentTimeMillis()

        val state = AppBus.appState.value

        // ✅ 先清理旧连接
        socketClient?.close()
        httpClient?.stop()
        socketClient = null
        httpClient = null

        if (state.powerSaveMode) {
            // 👉 HTTP 模式
            httpClient = OkxHttpClient(
                symbols = listOf(Symbol.ETH, Symbol.BTC),
                baseUrl = "http://hostname:8989",
                onPrice = ::onPrice,
                onState = { status ->
                    AppBus.update { state ->
                        state.copy(socketStatus = status)
                    }
                }
            )
            httpClient?.start()

        } else {
            // 👉 WebSocket 模式
            socketClient = OkxWebSocketClient(
                symbols = listOf(Symbol.ETH, Symbol.BTC),
                onPrice = ::onPrice,
//                onSocketState = { AppBus.update { it.copy(socketStatus = it) } }    //Argument type mismatch: actual type is 'AppState', but 'SocketStatus' was expected.
                onSocketState = { status ->
                    AppBus.update { state ->
                        state.copy(socketStatus = status)
                    }
                }
            )
            socketClient?.connect()
        }
    }

    private fun onPrice(symbol: Symbol, price: Double) {
        lastPriceMessageAt = System.currentTimeMillis()

        AppBus.update {
            val newPrices = it.prices.toMutableMap()
            newPrices[symbol] = price
            it.copy(
                prices = newPrices,
                socketStatus = SocketStatus.CONNECTED
            )
        }

        evaluateConditions(symbol, price)
        updateNotificationThrottled()
    }

    private fun stopMonitoring() {

        if (!AppBus.appState.value.isMonitoring) return

        socketClient?.close()
        httpClient?.stop()

        healthJob?.cancel()

        AlarmPlayer.stop()
        stopVibration()
        flashlight.stopFlashing()


        val stopFloatIntent = Intent(this, FloatWindowService::class.java).apply {
            action = FloatWindowService.ACTION_STOP
        }
        startService(stopFloatIntent)

        AppBus.update {
            it.copy(
                isMonitoring = false,
                socketStatus = SocketStatus.DISCONNECTED
            )
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotificationThrottled() {
        val now = System.currentTimeMillis()
        if (now - lastNotifyTime < 1000) return
        lastNotifyTime = now
        updateNotification()
    }

    private fun updateNotification() {
        val state = AppBus.appState.value

        val isEnable = state.notifyEnable

        var content = buildString {
            state.prices[Symbol.ETH]?.let { append("ETH: $it  ") }
            state.prices[Symbol.BTC]?.let { append("BTC: $it") }
        }
        content = if(isEnable) content else "通知已禁用"

        notificationManager.notify(1001, buildNotification(content))
    }

    private fun evaluateConditions(symbol: Symbol, price: Double) {
        alarmConditions.getAll().forEach { condition ->
            if (!condition.enabled || condition.symbol != symbol) return@forEach

            val matched = when (condition.direction) {
                Direction.GREATER_THAN -> price >= condition.targetPrice
                Direction.LESS_THAN -> price <= condition.targetPrice
            }

            if (matched && !condition.triggered) {
                alarmConditions.updateTriggered(condition.id, true)

                if (!AlarmPlayer.isRunning()) {
                    AlarmPlayer.start(this)
                    vibrate()
                    flash()
                }

            } else if (!matched && condition.triggered) {
                alarmConditions.updateTriggered(condition.id, false)
            }
        }
    }

    private fun flash() {
        if (AppBus.appState.value.flashlightEnabled) {
            flashlight.startFlashing()
        }
    }

    private fun vibrate() {
        if (!AppBus.appState.value.vibrationEnabled) return
        if (!vibrator.hasVibrator()) return

        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 400, 400),
            0
        )
        vibrator.vibrate(effect)
    }

    private fun stopVibration() {
        vibrator.cancel()
    }

    private fun observeNetwork() {
        serviceScope.launch {
            networkAbilityMonitor.isAvailable.collectLatest { ok ->
                if (ok) {
                    networkLostSince = null
                    AppBus.update { it.copy(networkStatus = NetworkStatus.AVAILABLE) }
                } else {
                    if (networkLostSince == null) {
                        networkLostSince = System.currentTimeMillis()
                    }
                    AppBus.update { it.copy(networkStatus = NetworkStatus.LOST_SHORT) }
                }
            }
        }
    }

    private fun watchStreamHealth() {
        healthJob?.cancel()

        healthJob = serviceScope.launch {
            while (isActive) {
                delay(1000)

                val now = System.currentTimeMillis()

                val stale = now - lastPriceMessageAt >= 60_000L

                if (stale && AppBus.appState.value.isMonitoring) {
                    AppBus.update { it.copy(networkStatus = NetworkStatus.LOST_LONG) }

                    // Todo

                    if (!AlarmPlayer.isRunning()) {
                        AlarmPlayer.start(applicationContext)
                        vibrate()
                        flash()
                    }

                }else{
                    // 网络一直正常, 或者刚刚恢复正常
                    AppBus.update { it.copy(networkStatus = NetworkStatus.AVAILABLE) }

//                    if (AlarmPlayer.isRunning()) {
//                        // 需要确定 仅仅是 因为网络引起的
//                        AlarmPlayer.start(applicationContext)
//                        vibrate()
//                        flash()
//                    }


                }
            }
        }
    }

    private fun buildNotification(content: String): Notification {

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )



        val stopIntent = Intent(this, PriceMonitorService::class.java)
            .setAction(Actions.STOP_MONITOR)
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopMonitorIntent = Intent(this, PriceMonitorService::class.java) .setAction(Actions.STOP_ALARM)
        val stopMonitorPendingIntent = PendingIntent.getService( this, 234, stopMonitorIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE )

        val killIntent = Intent(this, PriceMonitorService::class.java) .setAction(Actions.KILL_MONITOR)
        val killPendingIntent = PendingIntent.getService( this, 3, killIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE )

        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("Swap Alarm")
            .setContentText(content)
            .setSmallIcon(R.drawable.eth_bound)
            .setLargeIcon(
                BitmapFactory.decodeResource(resources, R.drawable.bitcoin_logo_svgrepo_com)
            )
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, "停止监控", stopPending)
            .addAction(0, "停止报警", stopMonitorPendingIntent)
            .addAction(0, "终结程序", killPendingIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Price Monitor",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        socketClient?.close()
        httpClient?.stop()
        AlarmPlayer.stop()
        stopVibration()
        flashlight.stopFlashing()
        super.onDestroy()
    }
}