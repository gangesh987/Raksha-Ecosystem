package com.rakshacall.safety.data.local.entities

/**
 * Complete set of 19 Room/SQLite entities for RakshaCall.
 * Provides strict typing, timestamps, foreign identifiers, and offline persistence.
 */

// 1. users
data class UserEntity(
    val id: String,
    val rakshaCallId: String,
    val phoneNumber: String,
    val createdAt: Long = System.currentTimeMillis(),
    val email: String? = null,
    val authMode: String = "LOCAL_DEV"
)

// 2. protection_sessions
data class ProtectionSessionEntity(
    val id: String,
    val startTime: Long,
    val endTime: Long? = null,
    val status: String = "ACTIVE",
    val peakRisk: Int = 0,
    val finalRisk: Int = 0,
    val highestStage: String = "CONTACT",
    val inputSource: String = "MICROPHONE",
    val safetyBrakeTriggered: Boolean = false,
    val totalTacticsDetected: Int = 0,
    val isDemoSession: Boolean = false
)

// 3. call_sessions
data class CallSessionEntity(
    val id: String,
    val sessionId: String,
    val callerName: String = "Unknown",
    val callerNumber: String = "",
    val platform: String = "RakshaCall Protected Call",
    val callType: String = "VIDEO", // VIDEO, AUDIO, SCREEN, MANUAL
    val durationSeconds: Long = 0L,
    val outcome: String = "PROTECTED",
    val timestamp: Long = System.currentTimeMillis()
)

// 4. media_sources
data class MediaSourceEntity(
    val id: String,
    val sessionId: String,
    val sourceType: String, // LOCAL_MICROPHONE, CAMERA, ANDROID_MEDIAPROJECTION, GOOGLE_MEET_MEDIA_API, BROWSER_CAPTURE, MANUAL_TEXT
    val connectionState: String = "DISCONNECTED",
    val sampleRate: Int = 16000,
    val isHardwareMuted: Boolean = false,
    val connectedAt: Long = System.currentTimeMillis()
)

// 5. transcript_events
data class TranscriptEventEntity(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val speaker: String,
    val text: String,
    val confidence: Float,
    val language: String = "en-IN"
)

// 6. tactic_events
data class TacticEventEntity(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val tactic: String,
    val confidence: Float,
    val riskContribution: Int,
    val evidenceText: String,
    val modelProvider: String = "LOCAL"
)

// 7. scam_stage_events
data class ScamStageEventEntity(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val fromStage: String,
    val toStage: String,
    val triggeringTactic: String,
    val reason: String
)

// 8. risk_events
data class RiskEventEntity(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val tactic: String,
    val confidence: Float,
    val riskContribution: Int,
    val evidenceText: String,
    val cumulativeRisk: Int
)

// 9. risk_points
data class RiskPointEntity(
    val sessionId: String,
    val timestamp: Long,
    val riskScore: Int
)

// 10. velocity_events
data class VelocityEventEntity(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val velocityLevel: String, // LOW, MODERATE, HIGH
    val tacticCount: Int,
    val weightedSum: Float,
    val durationMs: Long
)

// 11. trusted_contacts
data class TrustedContactEntity(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isEmergency: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val consentStatus: String = "CONSENTED",
    val lastAlertTimestamp: Long? = null
)

// 12. alert_events
data class AlertEventEntity(
    val id: String,
    val sessionId: String,
    val contactId: String = "",
    val timestamp: Long,
    val alertType: String, // HIGH_RISK_TRIGGER, SAFETY_BRAKE, VERIFICATION_FAILED
    val deliveryStatus: String = "ALERT_REQUESTED", // ALERT_REQUESTED, SENDING, SENT, DELIVERED, FAILED, CANCELLED, NOT_CONFIGURED
    val payload: String = ""
)

// 13. evidence_events
data class EvidenceEventEntity(
    val eventId: String,
    val sessionId: String,
    val timestamp: Long,
    val eventType: String,
    val payloadJson: String,
    val previousHash: String,
    val currentHash: String,
    val syncState: String = "LOCAL_ONLY",
    val serverId: String? = null,
    val version: Int = 1
)

// 14. consent_records
data class ConsentRecordEntity(
    val id: String,
    val permissionName: String,
    val grantedTimestamp: Long,
    val revokedTimestamp: Long? = null,
    val policyVersion: String = "2.0.0"
)

// 15. verification_events
data class VerificationEventEntity(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val stepNumber: Int,
    val stepName: String,
    val status: String, // NOT_STARTED, IN_PROGRESS, VERIFIED, UNABLE_TO_VERIFY
    val notes: String = ""
)

// 16. platform_connections
data class PlatformConnectionEntity(
    val id: String,
    val platformName: String, // WhatsApp, Google Meet, Teams, Zoom, Telegram, Signal, Browser, Other
    val protectionMethod: String,
    val isSupported: Boolean,
    val permissionStatus: String,
    val lastUsedAt: Long? = null
)

// 17. ai_events
data class AIEventEntity(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val providerName: String, // LOCAL, GROQ, GEMINI, SAFE_FALLBACK
    val modelName: String,
    val latencyMs: Long,
    val status: String, // SUCCESS, FAILED, OFFLINE, FALLBACK
    val errorDetails: String? = null
)

// 18. incident_reports
data class IncidentReportEntity(
    val id: String,
    val sessionId: String,
    val generatedAt: Long,
    val summaryJson: String,
    val humanReportText: String,
    val integrityStatus: String, // VERIFIED, TAMPERED
    val exportFormat: String = "JSON_AND_TEXT"
)

// 19. notification_events
data class NotificationEventEntity(
    val id: String,
    val timestamp: Long,
    val title: String,
    val body: String,
    val priority: String = "HIGH",
    val category: String = "RISK_ALERT",
    val isRead: Boolean = false
)
