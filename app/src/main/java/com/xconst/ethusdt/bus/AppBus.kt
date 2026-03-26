package com.xconst.ethusdt.bus

import com.xconst.ethusdt.store.SettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AppBus {
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    fun update(transform: (AppState) -> AppState) {
        _appState.value = transform(_appState.value)
    }
    fun applySettings(settings: SettingsData) {
        _appState.update {
            it.copy(
                isMuted = settings.isMuted,
                screenFlashEnabled = settings.screenFlashEnabled,
                vibrationEnabled = settings.vibrationEnabled,
                flashlightEnabled = settings.flashlightEnabled,
                powerSaveMode = settings.powerSaveMode,
                oledModeEnabled = settings.oledModeEnabled,
                floatEnable = settings.floatEnable,


            )
        }
    }
}