package com.example.dualcamerarecorder.camera

import android.content.Context
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner

class CameraManager(private val context: Context) {
    companion object {
        private const val TAG = "CameraManager"
    }

    fun getCameraProvider(callback: (ProcessCameraProvider) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                callback(cameraProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get camera provider", e)
            }
        }, context.mainExecutor)
    }

    fun getAvailableCameras(cameraProvider: ProcessCameraProvider): List<CameraInfo> {
        return cameraProvider.availableCameraInfos
    }

    fun hasDualCamera(cameraProvider: ProcessCameraProvider): Boolean {
        return cameraProvider.availableCameraInfos.size >= 2
    }

    fun getFrontAndBackCameras(cameraProvider: ProcessCameraProvider): Pair<CameraSelector?, CameraSelector?> {
        val backCamera = try {
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        } catch (e: Exception) {
            null
        }

        val frontCamera = try {
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
        } catch (e: Exception) {
            null
        }

        return Pair(backCamera, frontCamera)
    }
}
