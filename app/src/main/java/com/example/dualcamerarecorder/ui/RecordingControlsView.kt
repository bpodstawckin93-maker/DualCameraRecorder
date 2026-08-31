package com.example.dualcamerarecorder.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.dualcamerarecorder.databinding.ViewRecordingControlsBinding

class RecordingControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewRecordingControlsBinding.inflate(LayoutInflater.from(context), this, true)

    val startStopButton get() = binding.btnStartStop
    val switchCameraButton get() = binding.btnSwitchCamera
    val pauseResumeButton get() = binding.btnPauseResume
    val pipModeButton get() = binding.btnPipMode
    val recordingIndicator get() = binding.recordingIndicator
    val recordingTimeText get() = binding.recordingTime

    init {
        orientation = HORIZONTAL
    }

    fun setRecordingMode(isRecording: Boolean) {
        startStopButton.text = if (isRecording) "Stop Recording" else "Start Recording"
        pauseResumeButton.isEnabled = isRecording
        recordingIndicator.visibility = if (isRecording) VISIBLE else GONE
    }

    fun updateRecordingTime(timeMs: Long) {
        val seconds = timeMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        recordingTimeText.text = String.format(
            "%02d:%02d:%02d",
            hours,
            minutes % 60,
            seconds % 60
        )
    }
}
