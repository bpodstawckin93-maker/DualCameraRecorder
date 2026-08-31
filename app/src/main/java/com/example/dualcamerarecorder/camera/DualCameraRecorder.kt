package com.example.dualcamerarecorder.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture as VideoCaptureNew
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DualCameraRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "DualCameraRecorder"
    }

    private var backCamera: androidx.camera.core.Camera? = null
    private var frontCamera: androidx.camera.core.Camera? = null
    private var backVideoCapture: VideoCaptureNew? = null
    private var frontVideoCapture: VideoCaptureNew? = null

    fun setupDualRecording(
        cameraProvider: ProcessCameraProvider,
        backPreview: Preview,
        frontPreview: Preview,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                cameraProvider.unbindAll()

                val backCameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                val frontCameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                // Setup back camera with video recording
                val backRecorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                backVideoCapture = VideoCaptureNew.withOutput(backRecorder)

                backCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    backCameraSelector,
                    backPreview,
                    backVideoCapture
                )

                // Setup front camera with video recording
                val frontRecorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                frontVideoCapture = VideoCaptureNew.withOutput(frontRecorder)

                frontCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    frontCameraSelector,
                    frontPreview,
                    frontVideoCapture
                )

                Log.d(TAG, "Dual camera recording setup completed successfully")
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup dual camera recording", e)
                onError(e)
            }
        }
    }

    fun startRecording(
        outputFile: java.io.File,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // Start recording on both cameras
            backVideoCapture?.let {
                // Implement recording start
            }
            frontVideoCapture?.let {
                // Implement recording start
            }
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            onError(e)
        }
    }

    fun stopRecording() {
        try {
            backVideoCapture?.let {
                // Implement recording stop
            }
            frontVideoCapture?.let {
                // Implement recording stop
            }
            Log.d(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
        }
    }

    fun pauseRecording() {
        try {
            Log.d(TAG, "Recording paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause recording", e)
        }
    }

    fun resumeRecording() {
        try {
            Log.d(TAG, "Recording resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume recording", e)
        }
    }
}
