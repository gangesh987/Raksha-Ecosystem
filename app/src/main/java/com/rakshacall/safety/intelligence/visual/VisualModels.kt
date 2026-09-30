package com.rakshacall.safety.intelligence.visual

data class VideoFrameSample(
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val rotation: Int = 0,
    val source: String = "remote_video"
)

enum class VisualThreatType {
    PAYMENT_APP_DETECTED,
    QR_CODE_PROMPT,
    REMOTE_ACCESS_INTERFACE,
    CREDENTIAL_ENTRY_SCREEN,
    OFFICIAL_SEAL_OR_BADGE,
    UNKNOWN_VISUAL
}

data class VisualThreatSignal(
    val type: VisualThreatType,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val riskScoreBonus: Int
)
