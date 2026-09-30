package com.rakshacall.safety.intelligence.visual

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.webrtc.VideoFrame
import org.webrtc.VideoSink

class FrameSampler(
    private val source: String = "remote_video",
    @Volatile private var sampleIntervalMs: Long = 1500L
) : VideoSink {

    private val _samples = MutableSharedFlow<VideoFrameSample>(extraBufferCapacity = 32)
    val samples: SharedFlow<VideoFrameSample> = _samples.asSharedFlow()

    private var lastSampleTimestamp = 0L

    fun setAdaptiveInterval(intervalMs: Long) {
        this.sampleIntervalMs = intervalMs.coerceIn(300L, 5000L)
    }

    override fun onFrame(frame: VideoFrame) {
        val now = System.currentTimeMillis()
        if (now - lastSampleTimestamp < sampleIntervalMs) return
        lastSampleTimestamp = now

        val buffer = frame.buffer
        _samples.tryEmit(
            VideoFrameSample(
                timestamp = now,
                width = buffer.width,
                height = buffer.height,
                rotation = frame.rotation,
                source = source
            )
        )
    }
}
