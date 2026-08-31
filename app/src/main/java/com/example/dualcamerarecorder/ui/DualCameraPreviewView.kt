package com.example.dualcamerarecorder.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import com.example.dualcamerarecorder.databinding.ViewDualCameraPreviewBinding

class DualCameraPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewDualCameraPreviewBinding.inflate(LayoutInflater.from(context), this, true)

    val backPreviewView: PreviewView get() = binding.backPreview
    val frontPreviewView: PreviewView get() = binding.frontPreview

    fun setBackPreview(preview: Preview) {
        preview.surfaceProvider = backPreviewView.surfaceProvider
    }

    fun setFrontPreview(preview: Preview) {
        preview.surfaceProvider = frontPreviewView.surfaceProvider
    }

    fun setPipMode(enabled: Boolean) {
        if (enabled) {
            binding.frontPreview.apply {
                layoutParams = LayoutParams(200, 300).apply {
                    rightMargin = 16
                    bottomMargin = 16
                }
            }
        } else {
            binding.frontPreview.apply {
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
            }
        }
    }
}
