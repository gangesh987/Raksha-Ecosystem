package com.raksha.video.protection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class ProtectionState { DISABLED, CONSENT_REQUIRED, CONNECTING, CONNECTED, MONITORING, RISK_DETECTED, INTERVENTION, DISCONNECTING, DISCONNECTED, ERROR }
enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL, UNKNOWN }
data class ProtectionSession(val sessionId: String, val callId: String, val websocketUrl: String, val expiresAt: String?)
data class RiskEvent(val eventId: String, val sessionId: String, val level: RiskLevel, val score: Int?, val reasons: List<String>, val timestamp: Long, val confidence: Float? = null, val signals: List<String> = emptyList(), val stage: String? = null)
data class ProtectionEvent(val type: String, val callId: String, val sessionId: String? = null, val payload: Map<String, Any?> = emptyMap())
data class ConsentState(val conversationAnalysis: Boolean = false, val visualAnalysis: Boolean = false, val riskDetection: Boolean = false, val trustedContactAlerts: Boolean = false)
data class ProtectionTimelineEvent(val timestamp: Long, val title: String, val detail: String? = null)
interface RakshaProtectionBridge {
    suspend fun attachToCall(callId: String, consent: ConsentState): Result<ProtectionSession>
    suspend fun sendEvent(event: ProtectionEvent)
    suspend fun detachFromCall()
    fun observeState(): StateFlow<ProtectionState>
    fun observeRiskEvents(): Flow<RiskEvent>
    fun observeTimeline(): Flow<List<ProtectionTimelineEvent>>
}
