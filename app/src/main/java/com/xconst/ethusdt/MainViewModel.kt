package com.xconst.ethusdt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xconst.ethusdt.bus.AppBus
import com.xconst.ethusdt.bus.Direction
import com.xconst.ethusdt.bus.UiState
import com.xconst.ethusdt.store.AlarmConditions
import com.xconst.ethusdt.store.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AlarmConditions(application)
    private val settingsRepo = SettingsRepository(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {

        // conditions
        viewModelScope.launch {
            repo.conditions.collect { conditions ->
                _uiState.value = _uiState.value.copy(
                    conditions = conditions
                )
            }
        }

        // AppBus runtime state
        viewModelScope.launch {
            AppBus.appState.collect { appState ->
                _uiState.value = _uiState.value.copy(
                    prices = appState.prices,
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

                )
            }
        }

        // DataStore settings → AppBus
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                AppBus.applySettings(settings)
            }
        }
    }

    fun addCondition(symbol: Symbol, direction: Direction, value: Double) {
        repo.addCondition(symbol, direction, value)
    }

    fun deleteCondition(id: Long) {
        repo.deleteCondition(id)
    }

    fun toggleCondition(id: Long) {
        repo.toggleCondition(id)
    }

    // ===== Settings =====

    fun setMuted(value: Boolean) {
        viewModelScope.launch {
            settingsRepo.setMuted(value)
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
        android.util.Log.d("OLED_DEBUG", "fuck $value")
        viewModelScope.launch {
            settingsRepo.setOledMode(value)
        }

    }

    fun setFloatMode(value: Boolean) {
        android.util.Log.d("setFloatMode", "fuck $value")

        viewModelScope.launch {
            settingsRepo.setFloatMode(value)
        }

    }

}