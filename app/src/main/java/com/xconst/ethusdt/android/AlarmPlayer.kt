package com.xconst.ethusdt.android

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.provider.Settings
import android.util.Log
import com.xconst.ethusdt.bus.AppBus

object AlarmPlayer {

    private const val TAG = "ALARM"

    private var mediaPlayer: MediaPlayer? = null

    private var alarming = false

    @Synchronized
    fun start(context: Context) {

        if (alarming) {
            Log.d(TAG, "Alarm already running")
            return
        }

        if (AppBus.appState.value.isMuted) {
            AppBus.update { it.copy(isAlarming = true) }
            return
        }



        try {

            Log.d(TAG, "Start alarm")

            mediaPlayer = MediaPlayer.create(
                context.applicationContext,
                Settings.System.DEFAULT_ALARM_ALERT_URI
            ).apply {

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                isLooping = true

                start()
            }

            alarming = true

            AppBus.update { it.copy(isAlarming = true) }

        } catch (e: Exception) {

            Log.e(TAG, "Alarm start error", e)

        }
    }

    @Synchronized
    fun stop() {

        if (!alarming) return

        try {

            Log.d(TAG, "Stop alarm")

            mediaPlayer?.stop()

            mediaPlayer?.release()

        } catch (e: Exception) {

            Log.e(TAG, "Alarm stop error", e)

        }

        mediaPlayer = null

        alarming = false

        AppBus.update { it.copy(isAlarming = false) }
    }

    fun isRunning(): Boolean {

        return alarming

    }
}