package com.xconst.ethusdt.store

data class SettingsData(
    val isMuted: Boolean = true,
    val screenFlashEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val flashlightEnabled: Boolean = false,
    val powerSaveMode: Boolean = true,
    val oledModeEnabled: Boolean = false,
    val floatEnable: Boolean = false,
    val notifyEnable: Boolean = false,


    )