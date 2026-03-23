package com.xconst.ethusdt.store

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val context: Context
) {

    private object Keys {
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val SCREEN_FLASH_ENABLED = booleanPreferencesKey("screen_flash_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val FLASHLIGHT_ENABLED = booleanPreferencesKey("flashlight_enabled")

        val POWER_SAVE_MODE = booleanPreferencesKey("power_save_mode")

        val OLED_MODE_ENABLE =  booleanPreferencesKey("oled_mode_enable")
    }

    val settingsFlow: Flow<SettingsData> = context.dataStore.data.map { prefs ->

         SettingsData(
            isMuted = prefs[Keys.IS_MUTED] ?: true,
            screenFlashEnabled = prefs[Keys.SCREEN_FLASH_ENABLED] ?: false,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            flashlightEnabled = prefs[Keys.FLASHLIGHT_ENABLED] ?: false,
            powerSaveMode = prefs[Keys.POWER_SAVE_MODE] ?: false,
            oledModeEnabled = prefs[Keys.OLED_MODE_ENABLE] ?: false,

        )
    }

    suspend fun setMuted(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_MUTED] = value
        }
    }

    suspend fun setScreenFlashEnabled(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCREEN_FLASH_ENABLED] = value
        }
    }

    suspend fun setVibrationEnabled(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VIBRATION_ENABLED] = value
        }
    }

    suspend fun setFlashlightEnabled(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FLASHLIGHT_ENABLED] = value
        }
    }


    suspend fun setPowerSaveMode(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.POWER_SAVE_MODE] = value
        }
    }

    suspend fun setOledMode(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OLED_MODE_ENABLE] = value
        }
    }


    suspend fun saveAll(
        isMuted: Boolean,
        screenFlashEnabled: Boolean,
        vibrationEnabled: Boolean,
        flashlightEnabled: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_MUTED] = isMuted
            prefs[Keys.SCREEN_FLASH_ENABLED] = screenFlashEnabled
            prefs[Keys.VIBRATION_ENABLED] = vibrationEnabled
            prefs[Keys.FLASHLIGHT_ENABLED] = flashlightEnabled

        }
    }
}