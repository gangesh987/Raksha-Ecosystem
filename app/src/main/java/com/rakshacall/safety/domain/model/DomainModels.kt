package com.rakshacall.safety.domain.model

/**
 * Tactic categories identified in digital-arrest and coercive scam operations.
 */
enum class ScamTactic(
    val displayName: String,
    val defaultWeight: Int,
    val description: String
) {
    AUTHORITY_IMPERSONATION("Authority Impersonation", 15, "Claiming to represent law enforcement, regulatory, or judicial bodies (CBI, Police, Court, TRAI, Customs)."),
    CRIMINAL_ALLEGATION("Criminal Allegation / Fear", 15, "Alleging seized contraband, money laundering, bank fraud, arrest warrants, or FIRs."),
    URGENCY("Urgency", 10, "Demanding instant reaction without deliberation ('within 15 minutes', 'right now')."),
    ISOLATION("Isolation Tactic", 15, "Instructing the victim to remain alone, not inform family, stay in a closed room, or keep the camera active."),
    PAYMENT_DEMAND("Payment Demand", 20, "Demanding direct funds transfer, escrow security deposits, penalty clearance, or RBI verification transfers."),
    CREDENTIAL_PRESSURE("Credential / OTP Pressure", 20, "Coercing the disclosure of OTPs, UPI PINs, CVVs, netbanking passwords, or Aadhaar authentication."),
    REMOTE_ACCESS("Remote Access Pressure", 15, "Insisting on installing screen sharing or remote management tools (AnyDesk, TeamViewer, RustDesk, QuickSupport)."),
    SUSPICIOUS_LINK("Suspicious Action / Link", 10, "Pressuring user to open external verification links or APK downloads."),
    ESCALATION("Coercive Escalation", 10, "Threatening imminent physical arrest, asset seizure, or public humiliation.");

    val isIrreversibleAction: Boolean
        get() = this in setOf(PAYMENT_DEMAND, CREDENTIAL_PRESSURE, REMOTE_ACCESS)
}

/**
 * Scam progression stages in a digital-arrest coercion sequence.
 */
enum class ScamStage(val order: Int, val displayName: String, val description: String) {
    CONTACT(0, "Contact", "Initial contact and identity establishment."),
    AUTHORITY(1, "Authority", "Claiming legal/statutory authority over victim."),
    FEAR(2, "Fear & Allegation", "Inducing panic through fabricated criminal charges."),
    ISOLATION(3, "Isolation", "Severing external consultation and digital custody."),
    DEMAND(4, "Demand", "Pivoting to compliance and resolution requirements."),
    PAYMENT_CREDENTIAL(5, "Payment / Credential Pressure", "Demanding money transfer or credential disclosure."),
    ESCALATION(6, "Coercive Escalation", "Maximum pressure with immediate arrest threats.");

    companion object {
        fun fromOrder(order: Int): ScamStage = entries.firstOrNull { it.order == order } ?: CONTACT
    }
}

/**
 * Risk classification levels.
 */
enum class RiskLevel(val minScore: Int, val maxScore: Int, val label: String) {
    LOW(0, 30, "LOW RISK"),
    MEDIUM(31, 60, "MEDIUM RISK"),
    HIGH(61, 80, "HIGH RISK"),
    CRITICAL(81, 100, "CRITICAL RISK");

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score >= 81 -> CRITICAL
            score >= 61 -> HIGH
            score >= 31 -> MEDIUM
            else -> LOW
        }
    }
}

/**
 * Manipulation velocity levels.
 */
enum class VelocityLevel(val label: String, val description: String) {
    LOW("LOW", "Tactics observed with substantial intervals."),
    MODERATE("MODERATE", "Tactics accumulating with noticeable frequency."),
    HIGH("HIGH", "Multiple coercive tactics detected in rapid succession.");
}

/**
 * Media Source connection lifecycle states.
 */
enum class MediaSourceConnectionState {
    DISCONNECTED,
    REQUESTING_PERMISSION,
    CONNECTING,
    CONNECTED,
    STREAMING,
    PAUSED,
    ERROR,
    STOPPED
}

/**
 * Individual detected risk signal from a transcript snippet.
 */
data class RiskSignal(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tactic: ScamTactic,
    val confidence: Float,
    val riskContribution: Int,
    val evidenceText: String,
    val source: String = "SPEECH",
    val sessionId: String,
    val modelProvider: String = "LOCAL"
)

/**
 * Transcript event captured in real time.
 */
data class TranscriptEvent(
    val id: String,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val speaker: String = "CALLER", // CALLER, USER, UNKNOWN
    val text: String,
    val confidence: Float = 1.0f,
    val language: String = "en-IN"
)

/**
 * Real-time Protection Session.
 */
data class ProtectionSession(
    val id: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val peakRisk: Int = 0,
    val finalRisk: Int = 0,
    val highestStage: ScamStage = ScamStage.CONTACT,
    val inputSource: String = "MICROPHONE",
    val safetyBrakeTriggered: Boolean = false,
    val totalTacticsDetected: Int = 0,
    val isDemoSession: Boolean = false
)

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    INTERRUPTED
}

/**
 * Call session model.
 */
data class CallSession(
    val id: String,
    val sessionId: String,
    val callerName: String,
    val callerNumber: String,
    val platform: String,
    val callType: String = "VIDEO",
    val durationSeconds: Long = 0L,
    val outcome: String = "PROTECTED",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Verification state for independent verification coach.
 */
enum class VerificationStatus {
    NOT_STARTED,
    IN_PROGRESS,
    VERIFIED,
    UNABLE_TO_VERIFY
}

/**
 * Step within the Verification Coach.
 */
data class VerificationStep(
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val status: VerificationStatus = VerificationStatus.NOT_STARTED,
    val notes: String = ""
)

/**
 * Trusted contact model.
 */
data class TrustedContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isEmergency: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val consentStatus: String = "CONSENTED",
    val lastAlertTimestamp: Long? = null
)

/**
 * Honest delivery status for emergency alerts to trusted contacts.
 */
enum class AlertDeliveryStatus {
    ALERT_REQUESTED,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    CANCELLED,
    NOT_CONFIGURED,
    // Backwards compatibility aliases
    HANDOFF_COMPLETED,
    PROVIDER_ACCEPTED,
    DELIVERY_CONFIRMED;

    val isDelivered: Boolean get() = this == DELIVERED || this == SENT || this == DELIVERY_CONFIRMED
}

/**
 * Append-only tamper-evident evidence event.
 */
data class EvidenceEvent(
    val eventId: String,
    val sessionId: String = "session-default",
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val payloadJson: String,
    val previousHash: String,
    val currentHash: String = calculateHash(previousHash, eventId, timestamp, eventType, payloadJson)
) {
    companion object {
        const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

        fun calculateHash(previousHash: String, eventId: String, timestamp: Long, eventType: String, payload: String): String {
            val dataString = "$previousHash:$eventId:$timestamp:$eventType:$payload"
            val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(dataString.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

/**
 * User account representation.
 */
data class User(
    val id: String,
    val rakshaCallId: String,
    val phoneNumber: String,
    val createdAt: Long = System.currentTimeMillis(),
    val email: String? = null,
    val authMode: String = "LOCAL_DEV"
)

/**
 * Visual signal quality indicator.
 */
enum class VisualQuality {
    CLEAR,
    INCONCLUSIVE,
    ANOMALOUS
}

data class VisualSignal(
    val quality: VisualQuality,
    val faceDetected: Boolean,
    val faceStabilityScore: Float,
    val lightingScore: Float,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val statusMessage: String = "Visual signal requires verification"
)

/**
 * Fused risk assessment.
 */
data class FusedRiskAssessment(
    val overallRisk: Int,
    val riskLevel: RiskLevel,
    val scamStage: ScamStage,
    val manipulationVelocity: VelocityLevel,
    val velocityExplanation: String,
    val primarySignal: String,
    val supportingSignals: List<String>,
    val reasons: List<String>,
    val confidence: Float,
    val isSafetyBrakeRequired: Boolean,
    val disagreementDetected: Boolean,
    val disagreementExplanation: String?
)

/**
 * Rich explainable Risk Decision object.
 */
data class RiskDecision(
    val score: Int,
    val level: RiskLevel,
    val primarySignals: List<RiskSignal>,
    val supportingSignals: List<String>,
    val reasons: List<String>,
    val confidence: Float,
    val recommendedAction: String,
    val modelVersion: String = "2.0.0-PROD",
    val configVersion: String = "2.0.0"
)

/**
 * Granular user consent types.
 */
enum class ConsentType {
    MICROPHONE,
    CAMERA,
    SCREEN_CAPTURE,
    CLOUD_SYNC,
    TRANSCRIPT_STORAGE,
    EVIDENCE_STORAGE,
    TRUSTED_CONTACT_ALERTS,
    OPTIONAL_AI_PROCESSING
}

/**
 * Immutable consent audit record.
 */
data class ConsentRecord(
    val id: String,
    val type: ConsentType,
    val grantedAt: Long = System.currentTimeMillis(),
    val revokedAt: Long? = null,
    val policyVersion: String = "2.0.0"
)

/**
 * Offline-first sync states for cloud synchronization.
 */
enum class SyncState {
    LOCAL_ONLY,
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    CONFLICT
}

/**
 * In-App Protected Video Call Room.
 */
data class ProtectedRoom(
    val roomId: String, // e.g. RC-739201
    val hostName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isAudioMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isSpeakerOn: Boolean = true,
    val participantCount: Int = 1,
    val connectionQuality: String = "EXCELLENT",
    val status: String = "CONNECTED"
)

/**
 * Platform Connection representation for platform launcher.
 */
data class PlatformConnection(
    val id: String,
    val platformName: String,
    val supportedProtectionMethod: String,
    val permissionRequirements: String,
    val isSupported: Boolean,
    val isConfigured: Boolean,
    val limitationNote: String
)

/**
 * Exportable Incident Report.
 */
data class IncidentReport(
    val incidentId: String,
    val sessionId: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val peakRiskScore: Int,
    val finalRiskLevel: RiskLevel,
    val highestScamStage: ScamStage,
    val totalTacticsDetected: Int,
    val detectedTactics: List<RiskSignal>,
    val evidenceEvents: List<EvidenceEvent>,
    val isChainIntegrityVerified: Boolean,
    val summaryText: String,
    val humanReadableReport: String,
    val jsonExport: String
)
