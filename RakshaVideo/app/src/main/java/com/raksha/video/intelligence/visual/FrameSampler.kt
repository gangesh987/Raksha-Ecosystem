package com.raksha.video.intelligence.visual

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.webrtc.VideoFrame
import org.webrtc.VideoSink

class FrameSampler(private val source: String = "remote_video", private val intervalMs: Long = 1000L) : VideoSink {
    private val _samples = MutableSharedFlow<VideoFrameSample>(extraBufferCapacity = 16)
    val samples: SharedFlow<VideoFrameSample> = _samples
    private var lastTimestamp = 0L

    override fun onFrame(frame: VideoFrame) {
        val now = System.currentTimeMillis()
        if (now - lastTimestamp < intervalMs) return
        lastTimestamp = now
        val buffer = frame.buffer
        _samples.tryEmit(VideoFrameSample(now, buffer.width, buffer.height, frame.rotation, source))
    }
}
