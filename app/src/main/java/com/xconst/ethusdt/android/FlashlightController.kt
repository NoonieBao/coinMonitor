package com.xconst.ethusdt.android

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.*

class FlashlightController(context: Context) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraId: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var flashJob: Job? = null

    init {
        cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    fun startFlashing() {

        if (cameraId == null) return

        if (flashJob?.isActive == true) return

        flashJob = scope.launch {

            var on = false

            while (isActive) {

                on = !on

                cameraManager.setTorchMode(cameraId!!, on)

                delay(300)
            }
        }
    }

    fun stopFlashing() {

        flashJob?.cancel()

        flashJob = null

        cameraId?.let {
            cameraManager.setTorchMode(it, false)
        }
    }
}