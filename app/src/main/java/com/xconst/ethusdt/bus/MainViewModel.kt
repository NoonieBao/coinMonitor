package com.xconst.ethusdt.com.xconst.ethusdt.bus

import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xconst.ethusdt.Symbol
import com.xconst.ethusdt.bus.AppBus
import com.xconst.ethusdt.bus.CoinColor
import com.xconst.ethusdt.bus.Direction
import com.xconst.ethusdt.bus.NetworkStatus
import com.xconst.ethusdt.bus.UiState
import com.xconst.ethusdt.store.AlarmConditions
import com.xconst.ethusdt.store.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val conditionsRepo = AlarmConditions(application)
    private val settingsRepo = SettingsRepository(application)

    val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {

        // conditions
        viewModelScope.launch {
            conditionsRepo.conditions.collect { conditions ->
                _uiState.value = _uiState.value.copy(
                    conditions = conditions
                )
            }
        }

        // DataStore settings → AppBus
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                AppBus.applySettings(settings)
            }
        }


        // AppBus runtime state
        viewModelScope.launch {
            AppBus.appState.collect { appState ->

                val newColors :Map<Symbol, Color>  = Symbol.entries.associateWith { symbol ->
                    val newPrice = appState.prices[symbol] ?: 0.0
                    val oldPrice = _uiState.value.prices[symbol] ?: 0.0

                    when {
                        newPrice > oldPrice -> CoinColor.GREEN.rgb
                        newPrice < oldPrice -> CoinColor.RED.rgb
                        else -> _uiState.value.coinColors[symbol] ?: CoinColor.GRAY.rgb // 默认灰色或保持原样
                    }
                }


                _uiState.value = _uiState.value.copy(
                    prices = appState.prices,


                    coinColors = newColors,
                    socketStatus = appState.socketStatus,
                    networkStatus = appState.networkStatus,
                    isMonitoring = appState.isMonitoring,
                    isAlarming = appState.isAlarming,
                    isMuted = appState.isMuted,
                    screenFlashEnabled = appState.screenFlashEnabled,
                    vibrationEnabled = appState.vibrationEnabled,
                    flashlightEnabled = appState.flashlightEnabled,
                    powerSaveMode = appState.powerSaveMode,
                    oledModeEnabled = appState.oledModeEnabled,
                    floatEnable = appState.floatEnable,
                    notifyEnable = appState.notifyEnable,


                )
            }
        }


    }

    fun addCondition(symbol: Symbol, direction: Direction, value: Double) {
        conditionsRepo.addCondition(symbol, direction, value)
    }

    fun deleteCondition(id: Long) {
        conditionsRepo.deleteCondition(id)
    }

    fun toggleCondition(id: Long) {
        conditionsRepo.toggleCondition(id)
    }

    // ===== Settings =====

    fun setMuted(value: Boolean) {
        viewModelScope.launch {
            settingsRepo.setMuted(value)
        }
    }

    fun setNotifyEnable(value: Boolean) {
        viewModelScope.launch {
            settingsRepo.setNotifyEnable(value)
        }
    }

    fun setScreenFlashEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepo.setScreenFlashEnabled(value)
        }
    }

    fun setVibrationEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepo.setVibrationEnabled(value)
        }
    }

    fun setFlashlightEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepo.setFlashlightEnabled(value)
        }
    }

    fun setPowerSaveMode(value: Boolean) {
        viewModelScope.launch {
            settingsRepo.setPowerSaveMode(value)
        }
    }



    fun setOledMode(value: Boolean) {
        Log.d("OLED_DEBUG", "fuck $value")
        viewModelScope.launch {
            settingsRepo.setOledMode(value)
        }

    }

    fun setFloatMode(value: Boolean) {
        Log.d("setFloatMode", "fuck $value")

        viewModelScope.launch {
            settingsRepo.setFloatMode(value)
        }

    }

}