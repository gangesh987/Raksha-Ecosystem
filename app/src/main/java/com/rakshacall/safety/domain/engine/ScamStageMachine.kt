package com.rakshacall.safety.domain.engine

import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import java.util.UUID

/**
 * Transition event when scam stage progresses.
 */
data class StageTransition(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fromStage: ScamStage,
    val toStage: ScamStage,
    val triggeringTactic: ScamTactic,
    val reason: String
)

/**
 * Scam Stage Machine.
 * Strictly models the coercive digital-arrest sequence:
 * CONTACT -> AUTHORITY -> FEAR -> ISOLATION -> DEMAND -> PAYMENT_CREDENTIAL -> ESCALATION
 * Stage transitions only occur upon actual verified detected tactics.
 */
class ScamStageMachine {

    private var currentStage: ScamStage = ScamStage.CONTACT
    private val stageHistory = mutableListOf<StageTransition>()

    fun reset() {
        currentStage = ScamStage.CONTACT
        stageHistory.clear()
    }

    fun getCurrentStage(): ScamStage = currentStage

    fun getTransitions(): List<StageTransition> = stageHistory.toList()

    /**
     * Evaluate incoming risk signal and advance stage if appropriate.
     * Returns a StageTransition if a new higher stage was reached, or null if unchanged.
     */
    fun processSignal(signal: RiskSignal): StageTransition? {
        val candidateStage = mapTacticToStage(signal.tactic)

        // Monotonic progression: only advance forward
        if (candidateStage.order > currentStage.order) {
            val previous = currentStage
            currentStage = candidateStage
            val transition = StageTransition(
                sessionId = signal.sessionId,
                timestamp = signal.timestamp,
                fromStage = previous,
                toStage = candidateStage,
                triggeringTactic = signal.tactic,
                reason = "Detected ${signal.tactic.displayName} in live transcript: '${signal.evidenceText}'"
            )
            stageHistory.add(transition)
            return transition
        }
        return null
    }

    /**
     * Map tactic to the characteristic scam progression stage.
     */
    private fun mapTacticToStage(tactic: ScamTactic): ScamStage = when (tactic) {
        ScamTactic.AUTHORITY_IMPERSONATION -> ScamStage.AUTHORITY
        ScamTactic.CRIMINAL_ALLEGATION -> ScamStage.FEAR
        ScamTactic.URGENCY -> ScamStage.DEMAND
        ScamTactic.ISOLATION -> ScamStage.ISOLATION
        ScamTactic.PAYMENT_DEMAND -> ScamStage.PAYMENT_CREDENTIAL
        ScamTactic.CREDENTIAL_PRESSURE -> ScamStage.PAYMENT_CREDENTIAL
        ScamTactic.REMOTE_ACCESS -> ScamStage.PAYMENT_CREDENTIAL
        ScamTactic.SUSPICIOUS_LINK -> ScamStage.DEMAND
        ScamTactic.ESCALATION -> ScamStage.ESCALATION
    }
}
