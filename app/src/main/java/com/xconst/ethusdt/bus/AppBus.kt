package com.xconst.ethusdt.bus

import com.xconst.ethusdt.store.SettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AppBus {
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    fun applySettings(settings: SettingsData) {
        // 直接对 .value 进行赋值，逻辑更直观
        val current = _appState.value
        _appState.value = current.copy(
            isMuted = settings.isMuted,
            screenFlashEnabled = settings.screenFlashEnabled,
            vibrationEnabled = settings.vibrationEnabled,
            flashlightEnabled = settings.flashlightEnabled,
            powerSaveMode = settings.powerSaveMode,
            oledModeEnabled = settings.oledModeEnabled,
            floatEnable = settings.floatEnable
        )
    }
}

