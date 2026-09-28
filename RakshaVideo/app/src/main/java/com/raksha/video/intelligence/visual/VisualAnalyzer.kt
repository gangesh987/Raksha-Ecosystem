package com.raksha.video.intelligence.visual

class VisualAnalyzer {
    fun analyzeFrame(sample: VideoFrameSample): VisualSignal = VisualSignal(
        type = "FRAME_SAMPLE",
        confidence = 1.0f,
        timestamp = sample.timestamp,
        source = sample.source,
        metadata = mapOf("width" to sample.width, "height" to sample.height, "rotation" to sample.rotation)
    )
}
