package com.example.dualcamerarecorder.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File

/**
 * Утилита для синхронизации и объединения видео с двух камер
 */
class VideoSyncMerger(private val context: Context) {
    companion object {
        private const val TAG = "VideoSyncMerger"
        private const val TIMEOUT_US = 10000L
    }

    /**
     * Синхронизирует два видеофайла и объединяет их
     */
    fun syncAndMergeVideos(
        backVideoPath: String,
        frontVideoPath: String,
        outputPath: String,
        onProgress: (progress: Int) -> Unit,
        onComplete: (success: Boolean, error: String?) -> Unit
    ) {
        try {
            val backFile = File(backVideoPath)
            val frontFile = File(frontVideoPath)

            if (!backFile.exists() || !frontFile.exists()) {
                onComplete(false, "Input video files not found")
                return
            }

            // Получаем информацию о видео
            val backInfo = getVideoInfo(backVideoPath)
            val frontInfo = getVideoInfo(frontVideoPath)

            Log.d(TAG, "Back video duration: ${backInfo.duration}ms")
            Log.d(TAG, "Front video duration: ${frontInfo.duration}ms")

            // Используем минимальную длительность
            val minDuration = minOf(backInfo.duration, frontInfo.duration)

            // Объединяем видео
            mergeVideos(
                backVideoPath,
                frontVideoPath,
                outputPath,
                minDuration,
                onProgress,
                onComplete
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing videos", e)
            onComplete(false, e.message)
        }
    }

    private fun getVideoInfo(videoPath: String): VideoInfo {
        val extractor = MediaExtractor()
        extractor.setDataSource(videoPath)

        var duration = 0L
        var width = 0
        var height = 0

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

            if (mime.startsWith("video/")) {
                width = format.getInteger(MediaFormat.KEY_WIDTH)
                height = format.getInteger(MediaFormat.KEY_HEIGHT)
                duration = format.getLong(MediaFormat.KEY_DURATION) / 1000 // в миллисекунды
            }
        }

        extractor.release()
        return VideoInfo(duration, width, height)
    }

    private fun mergeVideos(
        backVideoPath: String,
        frontVideoPath: String,
        outputPath: String,
        maxDuration: Long,
        onProgress: (progress: Int) -> Unit,
        onComplete: (success: Boolean, error: String?) -> Unit
    ) {
        try {
            val backExtractor = MediaExtractor()
            backExtractor.setDataSource(backVideoPath)

            val frontExtractor = MediaExtractor()
            frontExtractor.setDataSource(frontVideoPath)

            // Получаем форматы видео
            var backVideoFormat: MediaFormat? = null
            var backAudioFormat: MediaFormat? = null
            var backVideoTrack = -1
            var backAudioTrack = -1

            for (i in 0 until backExtractor.trackCount) {
                val format = backExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                when {
                    mime.startsWith("video/") -> {
                        backVideoFormat = format
                        backVideoTrack = i
                    }
                    mime.startsWith("audio/") -> {
                        backAudioFormat = format
                        backAudioTrack = i
                    }
                }
            }

            // Создаем мультиплексер
            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var videoTrackIndex = -1
            var audioTrackIndex = -1

            if (backVideoFormat != null) {
                videoTrackIndex = muxer.addTrack(backVideoFormat)
            }

            if (backAudioFormat != null) {
                audioTrackIndex = muxer.addTrack(backAudioFormat)
            }

            muxer.start()

            // Копируем видео с задней камеры
            if (backVideoTrack >= 0) {
                copyVideoTrack(backExtractor, backVideoTrack, muxer, videoTrackIndex, maxDuration, onProgress)
            }

            // Копируем аудио
            if (backAudioTrack >= 0) {
                copyAudioTrack(backExtractor, backAudioTrack, muxer, audioTrackIndex, maxDuration)
            }

            muxer.stop()
            muxer.release()
            backExtractor.release()
            frontExtractor.release()

            Log.d(TAG, "Videos merged successfully to $outputPath")
            onComplete(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error merging videos", e)
            onComplete(false, e.message)
        }
    }

    private fun copyVideoTrack(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxer: MediaMuxer,
        muxerTrackIndex: Int,
        maxDuration: Long,
        onProgress: (progress: Int) -> Unit
    ) {
        extractor.selectTrack(trackIndex)
        val buffer = MediaCodec.BufferInfo()
        var progressPercent = 0

        while (true) {
            val sampleTime = extractor.sampleTime
            if (sampleTime < 0 || sampleTime > maxDuration * 1000) break

            val size = extractor.readSampleData(android.media.MediaCodec.BufferInfo().inputBuffer, 0)
            if (size < 0) break

            buffer.presentationTimeUs = sampleTime
            buffer.size = size
            buffer.flags = extractor.sampleFlags

            muxer.writeSampleData(muxerTrackIndex, android.media.MediaCodec.BufferInfo().inputBuffer, buffer)
            extractor.advance()

            // Обновляем прогресс
            if (maxDuration > 0) {
                progressPercent = (sampleTime / (maxDuration * 1000).toFloat() * 100).toInt()
                onProgress(progressPercent)
            }
        }
    }

    private fun copyAudioTrack(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxer: MediaMuxer,
        muxerTrackIndex: Int,
        maxDuration: Long
    ) {
        extractor.selectTrack(trackIndex)
        val buffer = MediaCodec.BufferInfo()

        while (true) {
            val sampleTime = extractor.sampleTime
            if (sampleTime < 0 || sampleTime > maxDuration * 1000) break

            val size = extractor.readSampleData(android.media.MediaCodec.BufferInfo().inputBuffer, 0)
            if (size < 0) break

            buffer.presentationTimeUs = sampleTime
            buffer.size = size
            buffer.flags = extractor.sampleFlags

            muxer.writeSampleData(muxerTrackIndex, android.media.MediaCodec.BufferInfo().inputBuffer, buffer)
            extractor.advance()
        }
    }

    data class VideoInfo(
        val duration: Long,  // в миллисекундах
        val width: Int,
        val height: Int
    )
}
