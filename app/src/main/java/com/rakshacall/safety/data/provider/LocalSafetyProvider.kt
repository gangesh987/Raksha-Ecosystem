package com.rakshacall.safety.data.provider

import com.rakshacall.safety.domain.engine.RiskEngine
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.provider.AIAnalysisResult
import com.rakshacall.safety.domain.provider.AIConnectionState
import com.rakshacall.safety.domain.provider.AIProvider

/**
 * Deterministic local safety provider acting as an independent guardrail.
 * Always available offline, zero-latency, zero-cloud dependency.
 */
class LocalSafetyProvider(
    private val riskEngine: RiskEngine = RiskEngine()
) : AIProvider {
    override val providerName: String = "RakshaCall-Deterministic-Guardrail"
    override val isAvailable: Boolean = true
    override val connectionState: AIConnectionState = AIConnectionState.CONNECTED

    override suspend fun analyze(transcript: String, context: List<String>): AIAnalysisResult {
        val signals = riskEngine.analyzeTranscript(transcript, System.currentTimeMillis())
        val irreversible = signals.any { it.tactic.isIrreversibleAction }
        val score: Int = signals.sumOf { it.riskContribution }
        val tactics = signals.map { it.tactic.name }

        
        val stage = when {
            signals.any { it.tactic == ScamTactic.PAYMENT_DEMAND || it.tactic == ScamTactic.CREDENTIAL_PRESSURE } -> "PAYMENT_CREDENTIAL"
            signals.any { it.tactic == ScamTactic.ISOLATION } -> "ISOLATION"
            signals.any { it.tactic == ScamTactic.CRIMINAL_ALLEGATION } -> "FEAR"
            signals.any { it.tactic == ScamTactic.AUTHORITY_IMPERSONATION } -> "AUTHORITY"
            else -> "CONTACT"
        }

        val reason = if (signals.isEmpty()) {
            "Normal conversational content. No coercive patterns detected."
        } else {
            "Detected ${signals.size} safety signals: ${tactics.joinToString(", ")}. ${if (irreversible) "Irreversible action requested." else ""}"
        }

        val action = if (irreversible) {
            "DO NOT transfer money or disclose OTP/credentials. Disconnect immediately."
        } else if (score >= 30) {

            "Verify caller identity independently via official department channels."
        } else {
            "Stay alert and never disclose private credentials."
        }

        return AIAnalysisResult(
            tactics = tactics,
            stage = stage,
            riskContribution = score.coerceIn(0, 100),
            confidence = if (signals.isNotEmpty()) 0.95f else 0.80f,
            reason = reason,
            irreversibleAction = irreversible,
            recommendedAction = action,
            modelVersion = "local-nlp-v2.1"
        )
    }
}
