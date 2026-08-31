package com.example.dualcamerarecorder.camera

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DualCameraVideoRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "DualCameraVideoRecorder"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var backCamera: androidx.camera.core.Camera? = null
    private var frontCamera: androidx.camera.core.Camera? = null
    private var backVideoCapture: VideoCapture<Recorder>? = null
    private var frontVideoCapture: VideoCapture<Recorder>? = null

    private var backRecording: androidx.camera.video.Recording? = null
    private var frontRecording: androidx.camera.video.Recording? = null
    
    private var isRecording = false
    private var isPaused = false

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
                backVideoCapture = VideoCapture.withOutput(backRecorder)

                backCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    backCameraSelector,
                    backPreview,
                    backVideoCapture!!
                )

                // Setup front camera with video recording
                val frontRecorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                frontVideoCapture = VideoCapture.withOutput(frontRecorder)

                frontCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    frontCameraSelector,
                    frontPreview,
                    frontVideoCapture!!
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
        onRecordingStart: () -> Unit,
        onRecordingStop: (String, String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            if (isRecording) {
                Log.w(TAG, "Already recording")
                return
            }

            isRecording = true
            isPaused = false

            val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())

            // Back camera file
            val backFile = File(
                context.getExternalFilesDir("videos"),
                "back_${timestamp}.mp4"
            )
            backFile.parentFile?.mkdirs()

            // Front camera file
            val frontFile = File(
                context.getExternalFilesDir("videos"),
                "front_${timestamp}.mp4"
            )
            frontFile.parentFile?.mkdirs()

            // Start back camera recording
            val backOutputOptions = FileOutputOptions.Builder(backFile).build()
            backRecording = backVideoCapture?.output
                ?.prepareRecording(context, backOutputOptions)
                ?.withAudioEnabled()
                ?.start(context.mainExecutor) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.d(TAG, "Back camera recording started")
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (!recordEvent.hasError()) {
                                Log.d(TAG, "Back camera recording saved: ${backFile.absolutePath}")
                            } else {
                                Log.e(TAG, "Back camera recording error: ${recordEvent.error}")
                            }
                        }
                        else -> {}
                    }
                }

            // Start front camera recording
            val frontOutputOptions = FileOutputOptions.Builder(frontFile).build()
            frontRecording = frontVideoCapture?.output
                ?.prepareRecording(context, frontOutputOptions)
                ?.withAudioEnabled()
                ?.start(context.mainExecutor) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.d(TAG, "Front camera recording started")
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (!recordEvent.hasError()) {
                                Log.d(TAG, "Front camera recording saved: ${frontFile.absolutePath}")
                            } else {
                                Log.e(TAG, "Front camera recording error: ${recordEvent.error}")
                            }
                        }
                        else -> {}
                    }
                }

            Log.d(TAG, "Recording started")
            onRecordingStart()
        } catch (e: Exception) {
            isRecording = false
            Log.e(TAG, "Failed to start recording", e)
            onError(e)
        }
    }

    fun stopRecording(
        onComplete: (backFile: String, frontFile: String) -> Unit
    ) {
        try {
            if (!isRecording) {
                Log.w(TAG, "Not recording")
                return
            }

            backRecording?.stop()
            frontRecording?.stop()
            
            isRecording = false
            isPaused = false

            val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())
            val backPath = File(context.getExternalFilesDir("videos"), "back_*.mp4").absolutePath
            val frontPath = File(context.getExternalFilesDir("videos"), "front_*.mp4").absolutePath

            Log.d(TAG, "Recording stopped")
            onComplete(backPath, frontPath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
        }
    }

    fun pauseRecording() {
        try {
            if (!isRecording || isPaused) return
            
            // Pause logic for API 31+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                backRecording?.pause()
                frontRecording?.pause()
                isPaused = true
                Log.d(TAG, "Recording paused")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause recording", e)
        }
    }

    fun resumeRecording() {
        try {
            if (!isRecording || !isPaused) return
            
            // Resume logic for API 31+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                backRecording?.resume()
                frontRecording?.resume()
                isPaused = false
                Log.d(TAG, "Recording resumed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume recording", e)
        }
    }

    fun isRecordingActive(): Boolean = isRecording
    fun isRecordingPaused(): Boolean = isPaused
}
