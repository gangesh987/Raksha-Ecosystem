package com.rakshacall.safety.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthSessionRequest(
    val rakshaCallId: String,
    val phoneNumber: String,
    val clientTimestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AuthSessionResponse(
    val token: String,
    val userId: String,
    val expiresAt: Long
)

@Serializable
data class CreateSessionRequest(
    val sessionId: String,
    val startTime: Long,
    val inputSource: String,
    val isDemoSession: Boolean = false
)

@Serializable
data class SessionResponse(
    val sessionId: String,
    val status: String,
    val peakRisk: Int = 0,
    val finalRisk: Int = 0
)

@Serializable
data class SessionEventRequest(
    val eventId: String,
    val sessionId: String,
    val timestamp: Long,
    val eventType: String,
    val payloadJson: String,
    val previousHash: String,
    val currentHash: String
)

@Serializable
data class EventResponse(
    val eventId: String,
    val acknowledged: Boolean,
    val serverTimestamp: Long
)

@Serializable
data class RiskAnalyzeRequest(
    val sessionId: String,
    val text: String,
    val speaker: String,
    val timestamp: Long
)

@Serializable
data class RiskAnalyzeResponse(
    val riskScore: Int,
    val detectedTactics: List<String>,
    val stage: String,
    val velocity: String,
    val safetyBrakeTriggered: Boolean
)

@Serializable
data class TimelineResponse(
    val sessionId: String,
    val events: List<SessionEventRequest>
)

@Serializable
data class VerifyEvidenceRequest(
    val sessionId: String,
    val lastKnownHash: String
)

@Serializable
data class VerifyEvidenceResponse(
    val sessionId: String,
    val isValid: Boolean,
    val compromisedEventId: String? = null
)

@Serializable
data class ExportEvidenceRequest(
    val sessionId: String,
    val format: String // "JSON", "TXT", "PDF"
)

@Serializable
data class ExportEvidenceResponse(
    val downloadUrl: String,
    val checksum: String,
    val generatedAt: Long
)

@Serializable
data class TrustedContactAlertRequest(
    val sessionId: String,
    val contactId: String,
    val riskScore: Int,
    val detectedTactics: List<String>,
    val timestamp: Long
)

@Serializable
data class TrustedContactAlertResponse(
    val alertId: String,
    val status: String, // ACCEPTED, DISPATCHED, DELIVERED
    val dispatchedAt: Long
)

@Serializable
data class SyncRequest(
    val lastSyncedTimestamp: Long,
    val pendingEvents: List<SessionEventRequest>
)

@Serializable
data class SyncResponse(
    val acknowledgedCount: Int,
    val serverTimestamp: Long,
    val hasMore: Boolean
)

@Serializable
data class RemoteAppConfigResponse(
    val minimumAppVersion: String,
    val riskThresholdHigh: Int = 60,
    val riskThresholdCritical: Int = 80,
    val emergencyHelpline: String = "1930",
    val officialCyberPortal: String = "https://cybercrime.gov.in",
    val supportedLanguages: List<String> = listOf("en-IN", "hi-IN")
)
