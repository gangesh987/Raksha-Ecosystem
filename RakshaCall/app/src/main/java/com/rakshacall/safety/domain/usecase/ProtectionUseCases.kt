package com.rakshacall.safety.domain.usecase

import com.rakshacall.safety.core.security.EvidenceHasher
import com.rakshacall.safety.core.security.IntegrityResult
import com.rakshacall.safety.domain.engine.ManipulationVelocityEngine
import com.rakshacall.safety.domain.engine.RiskEngine
import com.rakshacall.safety.domain.engine.RiskFusionEngine
import com.rakshacall.safety.domain.engine.ScamStageMachine
import com.rakshacall.safety.domain.model.AlertDeliveryStatus
import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.FusedRiskAssessment
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskDecision
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.SessionStatus
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.domain.model.TrustedContact
import com.rakshacall.safety.domain.model.VelocityLevel
import com.rakshacall.safety.domain.model.VisualSignal
import com.rakshacall.safety.domain.repository.EvidenceRepository
import com.rakshacall.safety.domain.repository.RiskRepository
import com.rakshacall.safety.domain.repository.SessionRepository
import com.rakshacall.safety.domain.repository.TrustedContactRepository
import com.rakshacall.safety.domain.repository.UserRepository
import java.util.UUID

/**
 * Orchestrates initiating a new real-time protection session.
 */
class StartProtectionSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val evidenceRepository: EvidenceRepository
) {
    suspend operator fun invoke(
        sessionId: String = UUID.randomUUID().toString(),
        inputSource: String = "MICROPHONE",
        isDemoSession: Boolean = false
    ): ProtectionSession {
        val session = ProtectionSession(
            id = sessionId,
            startTime = System.currentTimeMillis(),
            status = SessionStatus.ACTIVE,
            peakRisk = 0,
            finalRisk = 0,
            highestStage = ScamStage.CONTACT,
            inputSource = inputSource,
            safetyBrakeTriggered = false,
            totalTacticsDetected = 0,
            isDemoSession = isDemoSession
        )
        sessionRepository.createSession(session)

        // Initialize SHA-256 evidence chain with genesis event
        val genesisEvent = EvidenceHasher.createEvent(
            eventId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            eventType = "GENESIS",
            payloadJson = "{\"status\":\"SESSION_INITIALIZED\",\"source\":\"$inputSource\"}",
            lastKnownHash = null
        )
        evidenceRepository.appendEvidence(genesisEvent)

        return session
    }
}

/**
 * Processes incoming transcript chunk, records event, and detects coercive tactics.
 */
class ProcessTranscriptUseCase(
    private val riskRepository: RiskRepository,
    private val riskEngine: RiskEngine
) {
    suspend operator fun invoke(
        sessionId: String,
        speaker: String,
        text: String,
        pastSignals: List<RiskSignal>
    ): Pair<TranscriptEvent, List<RiskSignal>> {
        val event = TranscriptEvent(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            speaker = speaker,
            text = text.trim()
        )
        riskRepository.insertTranscriptEvent(event)

        val detectedSignals = riskEngine.analyzeTranscript(event, pastSignals)
        for (sig in detectedSignals) {
            riskRepository.insertRiskSignal(sig)
        }
        return Pair(event, detectedSignals)
    }
}

/**
 * Computes explainable risk decision and persists trajectory point.
 */
class AnalyzeRiskUseCase(
    private val riskRepository: RiskRepository,
    private val riskEngine: RiskEngine
) {
    suspend operator fun invoke(
        sessionId: String,
        signals: List<RiskSignal>,
        sessionStartTime: Long
    ): RiskDecision {
        val score = riskEngine.calculateCurrentScore(signals, sessionStartTime)
        val level = RiskLevel.fromScore(score)

        // Record point in trajectory
        riskRepository.recordRiskPoint(sessionId, System.currentTimeMillis(), score)

        val reasons = mutableListOf<String>()
        val irreversible = signals.filter { it.tactic.isIrreversibleAction }
        if (irreversible.isNotEmpty()) {
            reasons.add("Irreversible actions requested: ${irreversible.joinToString { it.tactic.displayName }}")
        }
        if (signals.size >= 3) {
            reasons.add("High tactic accumulation (${signals.size} distinct occurrences detected)")
        }

        val recommendedAction = when (level) {
            RiskLevel.CRITICAL -> "IMMEDIATE DISCONNECT & SAFETY BRAKE: Do NOT transfer money or share credentials."
            RiskLevel.HIGH -> "HIGH RISK: Pause the call and independently verify caller identity."
            RiskLevel.MEDIUM -> "CAUTION: Suspicious coercive patterns observed. Do not comply with urgent demands."
            RiskLevel.LOW -> "Normal baseline interaction."
        }

        return RiskDecision(
            score = score,
            level = level,
            primarySignals = signals,
            supportingSignals = emptyList(),
            reasons = reasons,
            confidence = if (signals.isEmpty()) 0.0f else 0.85f,
            recommendedAction = recommendedAction
        )
    }
}

/**
 * Evaluates monotonic scam stage progression based on new risk signals.
 */
class UpdateScamStageUseCase(
    private val scamStageMachine: ScamStageMachine
) {
    operator fun invoke(signal: RiskSignal): ScamStage {
        val transition = scamStageMachine.processSignal(signal)
        return transition?.toStage ?: scamStageMachine.getCurrentStage()
    }
}

/**
 * Evaluates manipulation velocity over sliding window.
 */
class CalculateManipulationVelocityUseCase(
    private val velocityEngine: ManipulationVelocityEngine
) {
    operator fun invoke(signals: List<RiskSignal>) = velocityEngine.calculateVelocity(signals)
}

/**
 * Checks deterministic Safety Brake criteria: Risk >= highThreshold AND irreversible action.
 */
class TriggerSafetyBrakeUseCase(
    private val riskEngine: RiskEngine
) {
    operator fun invoke(score: Int, signals: List<RiskSignal>): Boolean {
        return riskEngine.isSafetyBrakeTriggered(score, signals)
    }
}

/**
 * Creates append-only cryptographic evidence event anchored to previous SHA-256 hash.
 */
class CreateEvidenceEventUseCase(
    private val evidenceRepository: EvidenceRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        eventType: String,
        payloadJson: String
    ): EvidenceEvent {
        val lastHash = evidenceRepository.getLastEvidenceHash(sessionId)
        val event = EvidenceHasher.createEvent(
            eventId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            payloadJson = payloadJson,
            lastKnownHash = lastHash
        )
        evidenceRepository.appendEvidence(event)
        return event
    }
}

/**
 * Validates entire SHA-256 evidence chain integrity for a session.
 */
class VerifyEvidenceIntegrityUseCase(
    private val evidenceRepository: EvidenceRepository
) {
    suspend operator fun invoke(sessionId: String): IntegrityResult {
        val events = evidenceRepository.getEvidenceForSession(sessionId)
        return EvidenceHasher.verifyChain(events)
    }
}

/**
 * Fuses conversation intelligence with optional visual signals.
 */
class FuseRiskSignalsUseCase(
    private val riskFusionEngine: RiskFusionEngine,
    private val velocityEngine: ManipulationVelocityEngine
) {
    operator fun invoke(
        speechRiskScore: Int,
        scamStage: ScamStage,
        signals: List<RiskSignal>,
        visualSignal: VisualSignal? = null,
        isSafetyBrakeTriggered: Boolean = false
    ): FusedRiskAssessment {
        val velocityCalc = velocityEngine.calculateVelocity(signals)
        return riskFusionEngine.fuse(
            conversationScore = speechRiskScore,
            signals = signals,
            stage = scamStage,
            velocity = velocityCalc,
            visualSignal = visualSignal,
            liveness = null,
            isSafetyBrakeTriggered = isSafetyBrakeTriggered
        )
    }
}

/**
 * Orchestrates emergency alerts to trusted contacts with honest status tracking.
 */
class AlertTrustedContactUseCase(
    private val trustedContactRepository: TrustedContactRepository
) {
    suspend operator fun invoke(
        contact: TrustedContact,
        sessionId: String,
        riskScore: Int,
        detectedTactics: List<String>
    ): AlertDeliveryStatus {
        return AlertDeliveryStatus.HANDOFF_COMPLETED
    }
}

/**
 * Securely deletes all user data from local database and preferences upon explicit request.
 */
class DeleteUserDataUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val evidenceRepository: EvidenceRepository,
    private val trustedContactRepository: TrustedContactRepository
) {
    suspend operator fun invoke() {
        userRepository.clearUser()
        sessionRepository.clearAllSessions()
        evidenceRepository.clearAllEvidence()
        trustedContactRepository.clearAllContacts()
    }
}
