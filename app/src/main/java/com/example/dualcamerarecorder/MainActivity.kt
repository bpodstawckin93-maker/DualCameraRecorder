package com.example.dualcamerarecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dualcamerarecorder.camera.DualCameraVideoRecorder
import com.example.dualcamerarecorder.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import java.util.concurrent.Executor
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dualCameraRecorder: DualCameraVideoRecorder
    private val PERMISSION_REQUEST_CODE = 100
    private var recordingTimer: Job? = null
    private var recordingStartTime = 0L
    private var isPipMode = false

    private val requiredPermissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dualCameraRecorder = DualCameraVideoRecorder(this, this)
        setupUI()

        if (allPermissionsGranted()) {
            initializeCameras()
        } else {
            requestPermissions()
        }
    }

    private fun setupUI() {
        binding.controlsView.startStopButton.setOnClickListener {
            if (dualCameraRecorder.isRecordingActive()) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        binding.controlsView.pauseResumeButton.setOnClickListener {
            if (dualCameraRecorder.isRecordingPaused()) {
                dualCameraRecorder.resumeRecording()
                binding.controlsView.pauseResumeButton.text = "Pause"
            } else {
                dualCameraRecorder.pauseRecording()
                binding.controlsView.pauseResumeButton.text = "Resume"
            }
        }

        binding.controlsView.switchCameraButton.setOnClickListener {
            binding.previewView.swapCameras()
        }

        binding.controlsView.pipModeButton.setOnClickListener {
            isPipMode = !isPipMode
            binding.previewView.setPipMode(isPipMode)
            binding.controlsView.pipModeButton.text = if (isPipMode) "Full View" else "PiP"
            Log.d("MainActivity", "PiP mode: $isPipMode")
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            requiredPermissions,
            PERMISSION_REQUEST_CODE
        )
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                initializeCameras()
            } else {
                Toast.makeText(this, R.string.recording_permission_denied, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initializeCameras() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Check if device has at least 2 cameras
                if (cameraProvider.availableCameraInfos.size < 2) {
                    Toast.makeText(
                        this,
                        "Device does not have dual cameras",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Create preview for back camera
                val backPreview = Preview.Builder().build()
                binding.previewView.setBackPreview(backPreview)

                // Create preview for front camera
                val frontPreview = Preview.Builder().build()
                binding.previewView.setFrontPreview(frontPreview)

                // Setup dual camera recording
                dualCameraRecorder.setupDualRecording(
                    cameraProvider,
                    backPreview,
                    frontPreview,
                    onSuccess = {
                        Log.d("MainActivity", "Cameras initialized successfully")
                    },
                    onError = { e ->
                        Log.e("MainActivity", "Failed to setup cameras", e)
                        Toast.makeText(this, "Failed to initialize cameras", Toast.LENGTH_SHORT).show()
                    }
                )
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize cameras", e)
            Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startRecording() {
        dualCameraRecorder.startRecording(
            onRecordingStart = {
                recordingStartTime = SystemClock.elapsedRealtime()
                binding.controlsView.setRecordingMode(true)
                startRecordingTimer()
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
            },
            onRecordingStop = { backFile, frontFile ->
                Log.d("MainActivity", "Back: $backFile, Front: $frontFile")
            },
            onError = { e ->
                Log.e("MainActivity", "Recording error", e)
                Toast.makeText(this, "Recording error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun stopRecording() {
        recordingTimer?.cancel()
        dualCameraRecorder.stopRecording { backFile, frontFile ->
            binding.controlsView.setRecordingMode(false)
            binding.controlsView.recordingTimeText.text = "00:00:00"
            Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "Recording stopped. Back: $backFile, Front: $frontFile")
        }
    }

    private fun startRecordingTimer() {
        recordingTimer = kotlinx.coroutines.GlobalScope.launch {
            while (dualCameraRecorder.isRecordingActive()) {
                val elapsedTime = SystemClock.elapsedRealtime() - recordingStartTime
                runOnUiThread {
                    binding.controlsView.updateRecordingTime(elapsedTime)
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }
}
