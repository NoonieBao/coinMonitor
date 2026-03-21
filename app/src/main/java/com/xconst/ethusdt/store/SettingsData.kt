package com.xconst.ethusdt.store

data class SettingsData(
    val isMuted: Boolean = true,
    val screenFlashEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val flashlightEnabled: Boolean = false,
    val powerSaveMode: Boolean = false,  // 👈 必须有
    val oledModeEnabled: Boolean = false,
)