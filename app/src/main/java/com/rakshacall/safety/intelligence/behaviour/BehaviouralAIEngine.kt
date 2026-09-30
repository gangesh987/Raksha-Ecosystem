package com.rakshacall.safety.intelligence.behaviour

import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.intelligence.intent.InferredIntent

data class BehaviourMetrics(
    val authorityPressure: Float = 0.0f,
    val urgencyPressure: Float = 0.0f,
    val threatEscalation: Float = 0.0f,
    val isolationPressure: Float = 0.0f,
    val repetitionScore: Float = 0.0f,
    val financialPressure: Float = 0.0f,
    val credentialPressure: Float = 0.0f,
    val remoteAccessPressure: Float = 0.0f,
    val conversationControl: Float = 0.0f,
    val manipulationVelocity: Float = 0.0f,
    val overallBehaviouralRisk: Float = 0.0f
)

data class BehaviourTurn(
    val timestamp: Long,
    val intent: InferredIntent,
    val tactics: List<ScamTactic>,
    val speaker: String
)

/**
 * Behavioural AI Engine evaluating psychological coercion across rolling temporal windows (Phase 10).
 * Analyzes conversational control, repetition velocity, urgency, and coercive trajectory.
 */
class BehaviouralAIEngine(
    private val windowDurationMs: Long = 180_000L // 3-minute rolling observation window
) {

    private val history = mutableListOf<BehaviourTurn>()

    fun recordTurn(turn: BehaviourTurn) {
        val now = turn.timestamp
        history.add(turn)
        history.removeAll { now - it.timestamp > windowDurationMs }
    }

    fun calculateMetrics(): BehaviourMetrics {
        if (history.isEmpty()) return BehaviourMetrics()

        val remoteTurns = history.filter { it.speaker == "CALLER" || it.speaker == "REMOTE_CALLER" }
        if (remoteTurns.isEmpty()) return BehaviourMetrics()

        val count = remoteTurns.size.toFloat()

        // 1. Specific tactical pressures
        val authorityCount = remoteTurns.count { it.tactics.contains(ScamTactic.AUTHORITY_IMPERSONATION) || it.intent == InferredIntent.POSSIBLE_IDENTITY_COERCION }
        val urgencyCount = remoteTurns.count { it.tactics.contains(ScamTactic.URGENCY) }
        val threatCount = remoteTurns.count { it.tactics.contains(ScamTactic.CRIMINAL_ALLEGATION) || it.intent == InferredIntent.POSSIBLE_ARREST_THREAT }
        val isolationCount = remoteTurns.count { it.tactics.contains(ScamTactic.ISOLATION) || it.intent == InferredIntent.POSSIBLE_ISOLATION_DEMAND }
        val financialCount = remoteTurns.count { it.tactics.contains(ScamTactic.PAYMENT_DEMAND) || it.intent == InferredIntent.POSSIBLE_FINANCIAL_TRANSFER_REQUEST }
        val credentialCount = remoteTurns.count { it.tactics.contains(ScamTactic.CREDENTIAL_PRESSURE) || it.intent == InferredIntent.POSSIBLE_VERIFICATION_CODE_REQUEST }
        val remoteAccessCount = remoteTurns.count { it.tactics.contains(ScamTactic.REMOTE_ACCESS) || it.intent == InferredIntent.POSSIBLE_REMOTE_ACCESS_REQUEST }

        val authority = (authorityCount / count).coerceIn(0f, 1f)
        val urgency = (urgencyCount / count).coerceIn(0f, 1f)
        val threat = (threatCount / count).coerceIn(0f, 1f)
        val isolation = (isolationCount / count).coerceIn(0f, 1f)
        val financial = (financialCount / count).coerceIn(0f, 1f)
        val credential = (credentialCount / count).coerceIn(0f, 1f)
        val remoteAccess = (remoteAccessCount / count).coerceIn(0f, 1f)

        // 2. Repetition score: multiple requests of the same coercive demand
        val demands = remoteTurns.filter { it.intent.isCoercive }
        val repetition = if (demands.size >= 3) 0.85f else if (demands.size >= 2) 0.50f else 0.0f

        // 3. Manipulation velocity: rate of tactic accumulation over time
        val spanSec = if (history.size > 1) {
            ((history.last().timestamp - history.first().timestamp) / 1000L).coerceAtLeast(10L)
        } else {
            30L
        }
        val velocity = (demands.size.toFloat() / (spanSec / 60f)).coerceIn(0f, 3f) / 3f

        // 4. Conversation Control: Ratio of caller turns attempting coercive demands
        val control = (demands.size.toFloat() / count).coerceIn(0f, 1f)

        // 5. Composite behavioural risk
        val overall = (authority * 0.15f +
                urgency * 0.15f +
                threat * 0.20f +
                isolation * 0.20f +
                credential * 0.35f +
                financial * 0.30f +
                remoteAccess * 0.30f +
                velocity * 0.25f).coerceIn(0f, 1f)

        return BehaviourMetrics(
            authorityPressure = authority,
            urgencyPressure = urgency,
            threatEscalation = threat,
            isolationPressure = isolation,
            repetitionScore = repetition,
            financialPressure = financial,
            credentialPressure = credential,
            remoteAccessPressure = remoteAccess,
            conversationControl = control,
            manipulationVelocity = velocity,
            overallBehaviouralRisk = overall
        )
    }

    fun reset() {
        history.clear()
    }
}
