package com.xconst.ethusdt.bus

import androidx.compose.ui.graphics.Color
import com.xconst.ethusdt.Symbol
import kotlinx.serialization.Serializable

@Serializable
enum class Direction { GREATER_THAN, LESS_THAN }
enum class SocketStatus { IDLE, CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED }
enum class NetworkStatus { AVAILABLE, LOST_SHORT, LOST_LONG }

@Serializable
data class AlertCondition(
    val id: Long,
    val symbol: Symbol,
    val direction: Direction,
    val targetPrice: Double,
    val enabled: Boolean,
    val triggered: Boolean = false
)


enum class CoinColor(val rgb: Color){
    GREEN(Color(0xFF2fbe85)),
    RED(Color(0xFFf7455d)),
    GRAY(Color(0xFF898989))
}


data class AppState(
    val prices: Map<Symbol, Double> = emptyMap(),
    val socketStatus: SocketStatus = SocketStatus.IDLE,

    val networkStatus: NetworkStatus = NetworkStatus.LOST_SHORT,
    val triggerCondition: AlertCondition? = null,       // 只保留一个吧, 无所谓

    val isMonitoring: Boolean = false,
    val isAlarming: Boolean = false,
    val isMuted: Boolean = true,
    val screenFlashEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val flashlightEnabled: Boolean = false,
    val powerSaveMode: Boolean = false,
    val oledModeEnabled: Boolean = false,
    val floatEnable: Boolean = false,
    val notifyEnable: Boolean = false,



    )

data class UiState(
    val prices: Map<Symbol, Double> = emptyMap(),
    val coinColors: Map<Symbol, Color> = emptyMap(),        // 仅UI


    val socketStatus: SocketStatus = SocketStatus.IDLE,
    val networkStatus: NetworkStatus = NetworkStatus.LOST_SHORT,
    val isMonitoring: Boolean = false,
    val isAlarming: Boolean = false,
    val isMuted: Boolean = true,   // 新增
    val screenFlashEnabled: Boolean = false,  // 新增
    val conditions: List<AlertCondition> = emptyList(),
    val vibrationEnabled: Boolean = true,
    val flashlightEnabled: Boolean = false,
    val powerSaveMode: Boolean = false,
    val oledModeEnabled: Boolean = false,
    val floatEnable: Boolean = false,
    val notifyEnable: Boolean = false,
)