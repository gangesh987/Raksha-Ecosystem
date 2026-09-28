package com.raksha.video.intelligence.visual

data class VideoFrameSample(
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val rotation: Int = 0,
    val source: String = "remote_video"
)

data class VisualSignal(
    val type: String,
    val confidence: Float?,
    val timestamp: Long,
    val source: String,
    val metadata: Map<String, Any?> = emptyMap()
)

enum class LivenessResult { LIVE, SPOOF_SUSPECTED, INCONCLUSIVE, UNAVAILABLE }

enum class VisualState { DISABLED, SAMPLING, ANALYZING, UNAVAILABLE }

interface LivenessAnalyzer {
    suspend fun analyze(frame: VideoFrameSample): LivenessResult
}

class UnavailableLivenessAnalyzer : LivenessAnalyzer {
    override suspend fun analyze(frame: VideoFrameSample): LivenessResult = LivenessResult.UNAVAILABLE
}
