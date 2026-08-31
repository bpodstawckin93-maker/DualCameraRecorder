package com.example.dualcamerarecorder.ui

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import com.example.dualcamerarecorder.databinding.ViewDualCameraPreviewBinding
import android.view.LayoutInflater
import kotlin.math.sqrt

class DragDropScalablePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewDualCameraPreviewBinding.inflate(LayoutInflater.from(context), this, true)
    
    private var lastX = 0f
    private var lastY = 0f
    private var currentScale = 1f
    private val minScale = 0.5f
    private val maxScale = 3f
    private var isPipMode = false
    private val minWidth = 150
    private val minHeight = 200

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    val backPreviewView: PreviewView get() = binding.backPreview
    val frontPreviewView: PreviewView get() = binding.frontPreview

    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
        
        binding.frontPreview.setOnTouchListener { v, event ->
            if (isPipMode) {
                scaleGestureDetector.onTouchEvent(event)
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.rawX
                        lastY = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!scaleGestureDetector.isInProgress) {
                            val deltaX = event.rawX - lastX
                            val deltaY = event.rawY - lastY

                            val newX = v.x + deltaX
                            val newY = v.y + deltaY

                            // Ограничение движения в пределах родителя
                            val maxX = (parent as? FrameLayout)?.width?.toFloat() ?: 0f
                            val maxY = (parent as? FrameLayout)?.height?.toFloat() ?: 0f

                            v.x = newX.coerceIn(0f, maxX - v.width)
                            v.y = newY.coerceIn(0f, maxY - v.height)

                            lastX = event.rawX
                            lastY = event.rawY
                        }
                    }
                }
                true
            } else {
                false
            }
        }
    }

    fun setBackPreview(preview: androidx.camera.core.Preview) {
        preview.surfaceProvider = backPreviewView.surfaceProvider
    }

    fun setFrontPreview(preview: androidx.camera.core.Preview) {
        preview.surfaceProvider = frontPreviewView.surfaceProvider
    }

    fun setPipMode(enabled: Boolean) {
        isPipMode = enabled
        if (enabled) {
            binding.frontPreview.apply {
                val pipWidth = 250
                val pipHeight = 350
                
                layoutParams = LayoutParams(pipWidth, pipHeight).apply {
                    rightMargin = 16
                    bottomMargin = 16
                }
                
                currentScale = 1f
                scaleX = 1f
                scaleY = 1f
                elevation = 8f
            }
        } else {
            binding.frontPreview.apply {
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
                x = 0f
                y = 0f
                currentScale = 1f
                scaleX = 1f
                scaleY = 1f
            }
        }
    }

    fun swapCameras() {
        // Логика для обмена камер
        binding.frontPreview.apply {
            rotation = (rotation + 180) % 360f
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (isPipMode) {
                val scaleFactor = detector.scaleFactor
                currentScale *= scaleFactor
                currentScale = currentScale.coerceIn(minScale, maxScale)

                binding.frontPreview.apply {
                    // Масштабирование с позицией касания в центре
                    val pivotX = detector.focusX - x
                    val pivotY = detector.focusY - y
                    
                    pivotX = pivotX / width
                    pivotY = pivotY / height
                    
                    scaleX = currentScale
                    scaleY = currentScale
                }
                return true
            }
            return false
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return isPipMode
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (isPipMode && currentScale != 1f) {
                // Двойной тап - сброс масштаба
                currentScale = 1f
                binding.frontPreview.apply {
                    scaleX = 1f
                    scaleY = 1f
                }
                return true
            }
            return false
        }
    }
}
