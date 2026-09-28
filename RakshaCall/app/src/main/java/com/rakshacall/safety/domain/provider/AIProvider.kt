package com.rakshacall.safety.domain.provider

import kotlinx.serialization.Serializable

@Serializable
data class AIAnalysisResult(
    val tactics: List<String> = emptyList(),
    val stage: String = "CONTACT",
    val riskContribution: Int = 0,
    val confidence: Float = 0.0f,
    val reason: String = "",
    val irreversibleAction: Boolean = false,
    val recommendedAction: String = "",
    val modelVersion: String = "local-deterministic-guardrail",
    val provider: String = "LOCAL",
    val timestamp: Long = System.currentTimeMillis(),
    val latencyMs: Long = 0L
)

enum class AIConnectionState {
    CONNECTED,
    CONNECTING,
    RECONNECTING,
    OFFLINE,
    AI_UNAVAILABLE
}

interface AIProvider {
    val providerName: String
    val isAvailable: Boolean
    val connectionState: AIConnectionState
    suspend fun analyze(transcript: String, context: List<String> = emptyList()): AIAnalysisResult
}
