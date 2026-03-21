package com.xconst.ethusdt

import androidx.compose.ui.text.font.FontFamily
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


data class AppState(
    val prices: Map<Symbol, Double> = emptyMap(),
    val socketStatus: SocketStatus = SocketStatus.IDLE,
    val networkStatus: NetworkStatus = NetworkStatus.LOST_SHORT,
    val isMonitoring: Boolean = false,
    val isAlarming: Boolean = false,
    val isMuted: Boolean = true,   // 新增
    val screenFlashEnabled: Boolean = false,   // 新增
    val vibrationEnabled: Boolean = true,
    val flashlightEnabled: Boolean = false,
    val powerSaveMode: Boolean = false,
    val oledModeEnabled: Boolean = false,


)

data class UiState(
    val prices: Map<Symbol, Double> = emptyMap(),
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
)