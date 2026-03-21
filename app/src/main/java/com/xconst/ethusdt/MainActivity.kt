
package com.xconst.ethusdt


import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.xconst.ethusdt.AlertCondition
import com.xconst.ethusdt.Direction
import com.xconst.ethusdt.NetworkStatus
import com.xconst.ethusdt.SocketStatus
import com.xconst.ethusdt.Symbol
import com.xconst.ethusdt.UiState



import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    private val exitReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finishAffinity()
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(exitReceiver) } catch (e: Exception) {}
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 允许内容绘制到刘海区域 (如果有)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // 2. 设置全屏 (隐藏状态栏和导航栏)
        hideSystemBars()

        registerReceiver(
            exitReceiver,
            IntentFilter(Actions.ACTION_EXIT_APP),
            Context.RECEIVER_NOT_EXPORTED
        )

        setContent {
            val state by viewModel.uiState.collectAsState()

            MaterialTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {}
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                MainScreen(
                    state = state,
                    onStart = {
                        ContextCompat.startForegroundService(
                            this,
                            Intent(this, PriceMonitorService::class.java).setAction(Actions.START)
                        )
                    },
                    onStop = {
                        startService(Intent(this, PriceMonitorService::class.java).setAction(Actions.STOP_MONITOR))
                    },
                    onStopAlarm = {
                        startService(Intent(this, PriceMonitorService::class.java).setAction(Actions.STOP_ALARM))
                    },
                    onAddCondition = { symbol, dir, value -> viewModel.addCondition(symbol, dir, value) },
                    onDeleteCondition = { viewModel.deleteCondition(it) },
                    onToggleCondition = { viewModel.toggleCondition(it) },
                    onToggleMuted = { viewModel.setMuted(it) },
                    onToggleFlashScreen = { viewModel.setScreenFlashEnabled(it) },
                    onToggleVibration = { viewModel.setVibrationEnabled(it) },
                    onToggleFlashlight = { viewModel.setFlashlightEnabled(it) },
                    onExitApp = {
                        startService(Intent(this, PriceMonitorService::class.java).setAction(Actions.STOP_MONITOR))
                        finishAffinity()
                    },
                    onSetPowerSaveMode = { viewModel.setPowerSaveMode(it) },
                    onSetOledMode = {viewModel.setOledMode(it)}
                )
            }
        }
    }

    // 隐藏系统栏的封装方法
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                // 隐藏状态栏和导航栏
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                // 沉浸式：滑动手势显示后自动隐藏
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }
}

// 下面是 MainScreen 等组件 (保持逻辑一致并修复了 clickable 崩溃)


//@Composable
//fun PixelShifter(enabled: Boolean, content: @Composable () -> Unit) {
//    if (!enabled) {
//        content()
//        return
//    }
//
//    val infiniteTransition = rememberInfiniteTransition(label = "BurnInProtection")
//
//    // 横向移动：范围加大到 20dp，时间缩短到 3.5 秒
//    val offsetX by infiniteTransition.animateValue(
//        initialValue = (0).dp,
//        targetValue = 120.dp,
//        typeConverter = Dp.VectorConverter,
//        animationSpec = infiniteRepeatable(
//            animation = tween(3500, easing = FastOutSlowInEasing), // 使用非线性曲线，动作更灵动
//            repeatMode = RepeatMode.Reverse
//        ),
//        label = "x"
//    )
//
//    // 纵向移动：范围加大到 15dp，时间设为 4.8 秒（错开时间差，形成不规则路径）
//    val offsetY by infiniteTransition.animateValue(
//        initialValue = (0).dp,
//        targetValue = 115.dp,
//        typeConverter = Dp.VectorConverter,
//        animationSpec = infiniteRepeatable(
//            animation = tween(4800, easing = LinearOutSlowInEasing),
//            repeatMode = RepeatMode.Reverse
//        ),
//        label = "y"
//    )
//
//    Box(modifier = Modifier.offset(x = offsetX, y = offsetY)) {
//        content()
//    }
//}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    state: UiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onStopAlarm: () -> Unit,
    onAddCondition: (Symbol, Direction, Double) -> Unit,
    onDeleteCondition: (Long) -> Unit,
    onToggleCondition: (Long) -> Unit,
    onToggleMuted: (Boolean) -> Unit,
    onToggleFlashScreen: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleFlashlight: (Boolean) -> Unit,
    onSetPowerSaveMode: (Boolean) -> Unit,
    onSetOledMode: (Boolean) -> Unit,
    onExitApp: () -> Unit
) {
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var flash by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var statusCollapsed by remember { mutableStateOf(false) }
    var panelCollapsed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isAllGood = state.isMonitoring && !state.isAlarming &&
            state.networkStatus == NetworkStatus.AVAILABLE &&
            state.socketStatus == SocketStatus.CONNECTED
    val hasIssue = !isAllGood

    LaunchedEffect(hasIssue) {
        if (hasIssue) {
            statusCollapsed = false
            panelCollapsed = false
        }
    }

    LaunchedEffect(state.isAlarming) {
        if (state.isAlarming) {
            while (state.isAlarming) {
                flash = !flash
                delay(500)
            }
        } else {
            flash = false
        }
    }

    LaunchedEffect(state.isMonitoring) {
        val activity = context as? Activity ?: return@LaunchedEffect
        if (state.isMonitoring) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    LaunchedEffect(Unit) {
        if (!state.isMonitoring) {
            onStart()
        }
    }
    Scaffold(
        containerColor = Color.Black,
        floatingActionButton = {
            if (!state.oledModeEnabled) {
                FloatingActionButton(onClick = { showDialog = true }) { Text("+") }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        state.screenFlashEnabled && flash -> Color.Red
                        state.oledModeEnabled -> Color.Black
                        else -> Color.White
                    }
                )
                .padding(padding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (state.oledModeEnabled) {
//                        AppBus.update { it.copy(oledModeEnabled = false) }
                        val currentTime = System.currentTimeMillis()
                        // 如果两次点击间隔超过 500ms，重置计数器
                        if (currentTime - lastClickTime > 500) {
                            clickCount = 1
                        } else {
                            clickCount++
                        }
                        lastClickTime = currentTime

                        // 满 3 次触发
                        if (clickCount >= 3) {
                            AppBus.update { it.copy(oledModeEnabled = false) }
                            clickCount = 0 // 触发后重置
                        }
                    }
                },
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.oledModeEnabled) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { statusCollapsed = !statusCollapsed }
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            val title = when {
                                statusCollapsed && isAllGood -> "✅ 一切正常"
                                statusCollapsed && hasIssue -> "❎ 察觉到异常"
                                else -> "🔽 系统状态"
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (statusCollapsed && isAllGood) Color(0xFF2E7D32)
                                else if (statusCollapsed && hasIssue) Color(0xFFC62828)
                                else Color.Unspecified
                            )
                            if (!statusCollapsed) {
                                Spacer(Modifier.height(8.dp))
                                Text("Socket: ${state.socketStatus}")
                                Text("Network: ${state.networkStatus}")
                                Text(if (state.isMonitoring) "✅ 监控运行中" else "❌ 监控未启动")
                                Text(if (state.isAlarming) "❌ 报警触发" else "✅ 无报警")
                            }
                        }
                    }
                }
            }

            item { PricePanel(state.prices, state.oledModeEnabled) }

            if (!state.oledModeEnabled) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { panelCollapsed = !panelCollapsed }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (panelCollapsed) "▶ 控制面板" else "▼ 控制面板",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (!panelCollapsed) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(onClick = onStart, enabled = !state.isMonitoring) { Text("开始监控") }
                                    OutlinedButton(onClick = onStop, enabled = state.isMonitoring) { Text("停止监控") }
                                    Button(onClick = onStopAlarm, enabled = state.isAlarming) { Text("停止报警") }
                                    Button(onClick = { onToggleMuted(!state.isMuted) }) {
                                        Text(if (state.isMuted) "🔇 静音" else "🔊 声音")
                                    }
                                    Button(onClick = { onToggleFlashScreen(!state.screenFlashEnabled) }) {
                                        Text(if (state.screenFlashEnabled) "💡 闪屏开" else "💡 闪屏关")
                                    }
                                    Button(onClick = { onToggleVibration(!state.vibrationEnabled) }) {
                                        Text(if (state.vibrationEnabled) "📳 震动开" else "📳 震动关")
                                    }
                                    Button(onClick = { onSetPowerSaveMode(!state.powerSaveMode) }) {
                                        Text(if (state.powerSaveMode) "⚡ 省电" else "🚀 实时")
                                    }
                                    Button(onClick = { onSetOledMode(!state.oledModeEnabled)  }) {
                                        Text("🌙 夜间模式")
                                    }
                                    Button(onClick = onExitApp) { Text("退出程序") }
                                }
                            }
                        }
                    }
                }

                item { Text("价格提醒", style = MaterialTheme.typography.titleMedium) }

                if (state.conditions.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("没有设置任何提醒")
                        }
                    }
                } else {
                    items(state.conditions, key = { it.id }) { condition ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${condition.symbol.displayName} ${if (condition.direction == Direction.GREATER_THAN) ">" else "<"} ${condition.targetPrice}")
                                    Text(if (condition.enabled) "已启用" else "已停用", style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    TextButton(onClick = { onToggleCondition(condition.id) }) { Text(if (condition.enabled) "禁用" else "启用") }
                                    TextButton(onClick = { onDeleteCondition(condition.id) }) { Text("删除", color = Color.Red) }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider()
                    Text(
                        text = "风险提示：行情有风险，本工具不构成投资建议。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        AddConditionDialog(
            onDismiss = { showDialog = false },
            onConfirm = { symbol, dir, value ->
                onAddCondition(symbol, dir, value)
                showDialog = false
            }
        )
    }
}

@Composable
fun PixelShifter(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }

    // 手动维护位移状态，减少 Compose 动画引擎的负担
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }

    LaunchedEffect(enabled) {
        var angle = 0.0
        while (enabled) {
            // 每 200ms 更新一次位置 (相当于 5 FPS)
            // 20dp 是摆动幅度，可以根据需要调整
            offsetX = (Math.sin(angle) * 20).dp
            offsetY = (Math.cos(angle * 0.8) * 15).dp

            angle += 0.1 // 步进值，决定移动速度
            kotlinx.coroutines.delay(200) // 👈 关键：控制刷新频率
        }
    }

    Box(modifier = Modifier.offset(x = offsetX, y = offsetY)) {
        content()
    }
}

@Composable
fun PricePanel(prices: Map<Symbol, Double>, oledMode: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Symbol.entries.forEach { symbol ->
            val price = prices[symbol]
            PixelShifter(enabled = oledMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(),
//                    colors = CardDefaults.cardColors(
//                        containerColor = if (oledMode) Color.Black else Color.White
//                    ),
                    colors = if (oledMode) {
                        // OLED 模式：强制纯黑省电
                        CardDefaults.cardColors(containerColor = Color.Black)
                    } else {
                        // 正常模式：使用系统默认卡片颜色（通常是浅灰色或 Material 容器色）
                        CardDefaults.cardColors()
                    },
//                    border = if (oledMode) androidx.compose.foundation.BorderStroke(0.5.dp, Color.DarkGray) else null
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            if(!oledMode){
//                                Icon(
//                                    painter = painterResource(getCryptoIcon(symbol)),
//                                    contentDescription = null,
//                                    modifier = Modifier.size(24.dp),
//                                    tint = if (oledMode) Color.Gray.copy(alpha = 0.5f) else Color.Unspecified
//                                )
//                            }

                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = symbol.okxInstId,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (oledMode) Color.DarkGray else Color.Unspecified
                            )
                        }
                        Text(
                            text = price?.toString() ?: "--",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (oledMode) Color(0xFF008800) else Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}

//fun getCryptoIcon(symbol: Symbol): Int {
//    return when (symbol) {
//        Symbol.BTC -> R.drawable.bitcoin_logo_svgrepo_com
//        Symbol.ETH -> R.drawable.eth_svgrepo_com
//        else -> R.drawable.bitcoin_logo_svgrepo_com
//    }
//}

fun getCryptoIcon(symbol: Symbol): Int {
    // 使用 Android 系统自带的图标作为占位符测试
    return when (symbol) {
        Symbol.BTC -> android.R.drawable.ic_menu_compass // 临时替换
        Symbol.ETH -> android.R.drawable.ic_menu_directions // 临时替换
        else -> android.R.drawable.ic_menu_info_details
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConditionDialog(onDismiss: () -> Unit, onConfirm: (Symbol, Direction, Double) -> Unit) {
    var valueText by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(Direction.GREATER_THAN) }
    var symbol by remember { mutableStateOf(Symbol.ETH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { valueText.toDoubleOrNull()?.let { onConfirm(symbol, direction, it) } }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text("添加提醒") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = symbol == Symbol.ETH, onClick = { symbol = Symbol.ETH }, label = { Text("ETH") })
                    FilterChip(selected = symbol == Symbol.BTC, onClick = { symbol = Symbol.BTC }, label = { Text("BTC") })
                }
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("价格") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = direction == Direction.GREATER_THAN, onClick = { direction = Direction.GREATER_THAN }, label = { Text("大于") })
                    FilterChip(selected = direction == Direction.LESS_THAN, onClick = { direction = Direction.LESS_THAN }, label = { Text("小于") })
                }
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    val dummyState = UiState(
        prices = mapOf(
            Symbol.ETH to 3500.0,
            Symbol.BTC to 68000.0
        ),
        socketStatus = SocketStatus.CONNECTED,
        networkStatus = NetworkStatus.AVAILABLE,
        isMonitoring = true,
        isAlarming = false,
        isMuted = false,
        screenFlashEnabled = false,
        vibrationEnabled = true,
        flashlightEnabled = false,
        conditions = listOf(
            AlertCondition(
                id = 1,
                symbol = Symbol.ETH,
                direction = Direction.GREATER_THAN,
                targetPrice = 3600.0,
                enabled = true,
                triggered = false
            ),
            AlertCondition(
                id = 2,
                symbol = Symbol.BTC,
                direction = Direction.LESS_THAN,
                targetPrice = 65000.0,
                enabled = true,
                triggered = false
            )
        )
    )

    MainScreen(
        state = dummyState,
        onStart = {},
        onStop = {},
        onStopAlarm = {},
        onAddCondition = { _, _, _ -> },
        onDeleteCondition = {},
        onToggleCondition = {},
        onToggleMuted = {},
        onToggleFlashScreen = {},
        onToggleVibration = {},
        onToggleFlashlight = {},
        onSetPowerSaveMode = {},
        onSetOledMode = {},
        onExitApp = {}
    )
}