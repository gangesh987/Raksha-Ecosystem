package com.rakshacall.safety.intelligence.visual

data class VideoFrameSample(
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val rotation: Int = 0,
    val source: String = "remote_video",
    val meanLuminance: Float = 0f,
    val isStaticFrame: Boolean = false
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1.0f
}

enum class VisualThreatType {
    PAYMENT_APP_DETECTED,
    QR_CODE_PROMPT,
    REMOTE_ACCESS_INTERFACE,
    CREDENTIAL_ENTRY_SCREEN,
    OFFICIAL_SEAL_OR_BADGE,
    DOCUMENT_INSPECTION_SCREEN,
    SCREEN_SHARE_DETECTED,
    UNKNOWN_VISUAL
}

data class VisualThreatSignal(
    val type: VisualThreatType,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val riskScoreBonus: Int,
    val source: String = "vision_engine"
)
