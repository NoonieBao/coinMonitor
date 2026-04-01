package com.xconst.ethusdt.floatWindows

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.xconst.ethusdt.com.xconst.ethusdt.bus.MainViewModel
import com.xconst.ethusdt.bus.AppBus
import com.xconst.ethusdt.Symbol
import com.xconst.ethusdt.bus.CoinColor
import com.xconst.ethusdt.bus.UiState
import kotlin.getValue

class FloatWindowService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private val lifecycleOwner = MyLifecycleOwner()

    companion object {
        const val ACTION_STOP = "com.xconst.ethusdt.STOP_FLOAT"
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }
    private lateinit var viewModel: MainViewModel

    override fun onCreate() {
        super.onCreate()

        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }
        viewModel = MainViewModel(application)

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                params.x += dragAmount.x.toInt()
                                params.y += dragAmount.y.toInt()
                                try {
                                    windowManager?.updateViewLayout(this@apply, params)
                                } catch (e: Exception) {}
                            }
                        }
                ) {
                    FloatingPriceUI(onClose = {
                        // 通过发送 Intent 停止自己
                        val stopIntent = Intent(this@apply.context, FloatWindowService::class.java).apply {
                            action = ACTION_STOP
                        }
                        this@apply.context.startService(stopIntent)
                    },viewModel)
                }
            }
        }

        windowManager?.addView(floatingView, params)
    }



    override fun onDestroy() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycleOwner.viewModelStore.clear()

        floatingView?.let {
            if (it.isAttachedToWindow) {
                windowManager?.removeView(it)
            }
        }
        super.onDestroy()
    }
}

@Composable
fun FloatingPriceUI(onClose: () -> Unit,viewModel : MainViewModel) {


//    val state by AppBus.appState.collectAsState()
    val state by viewModel.uiState.collectAsState()

    var floatStatus by remember { mutableStateOf(true) }
    val prices: Map<Symbol, Double> = state.prices

    Surface(
        color = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(8.dp), // 减小圆角半径更显硬朗
        shadowElevation = 4.dp
    ) {
        Box {
            // 只有展开状态才显示关闭按钮，防止折叠时太拥挤
            if (floatStatus) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp) // 缩小按钮尺寸
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 10.sp, // 缩小 X 字体
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 内容区域
            Column(
                modifier = Modifier
                    .padding(
                        // 紧凑型 Padding 策略
                        start = 8.dp,
                        top = 6.dp,
                        end = if (floatStatus) 24.dp else 8.dp, // 展开时给 X 留空间，折叠时对称
                        bottom = 6.dp
                    )
                    .width(IntrinsicSize.Max)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        floatStatus = !floatStatus
                    },
                verticalArrangement = Arrangement.spacedBy(4.dp) // 减小间距
            ) {
                val displaySymbols = if (floatStatus) Symbol.entries else Symbol.entries.filter { it == Symbol.ETH }

                displaySymbols.forEachIndexed { index, symbol ->
                    val price = prices[symbol]
                    Column {
                        if (floatStatus) {
                            Text(
                                text = symbol.okxInstId.take(8), // 安全截取
                                fontSize = 10.sp, // 极简字体
                                color = Color.Gray
                            )
                        }

                        Text(
                            text = price?.toString() ?: "--",
                            fontSize = 15.sp, // 缩小主要字体
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
//                            color = Color(0xFF00FF00)
                            color = state.coinColors[symbol]?: CoinColor.GRAY.rgb
                        )
                    }

                    if (floatStatus && index < displaySymbols.size - 1) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }
        }
    }
}
