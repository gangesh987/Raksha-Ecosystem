package com.rakshacall.safety.domain.engine

import com.rakshacall.safety.domain.model.FusedRiskAssessment
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.VelocityLevel
import com.rakshacall.safety.domain.model.VisualQuality
import com.rakshacall.safety.domain.model.VisualSignal

/**
 * Detects discrepancies between the primary conversational signal and supporting visual signals.
 */
object ModelDisagreementGuard {

    data class DisagreementResult(
        val isDisagreement: Boolean,
        val explanation: String?
    )

    fun checkDisagreement(
        conversationRiskLevel: RiskLevel,
        visualSignal: VisualSignal?,
        livenessAssessment: LivenessAssessment?
    ): DisagreementResult {
        if (visualSignal == null || livenessAssessment == null) {
            return DisagreementResult(isDisagreement = false, explanation = null)
        }

        val highConversation = conversationRiskLevel == RiskLevel.HIGH || conversationRiskLevel == RiskLevel.CRITICAL
        val visualNormal = visualSignal.quality == VisualQuality.CLEAR && livenessAssessment.status == "CLEAR"

        if (highConversation && visualNormal) {
            return DisagreementResult(
                isDisagreement = true,
                explanation = "Conversation evidence indicates significant coercive behavior. Supporting visual signal appears clear/normal. In accordance with safety protocol, conversational coercion remains PRIMARY."
            )
        }

        val lowConversation = conversationRiskLevel == RiskLevel.LOW
        val visualAnomalous = visualSignal.quality == VisualQuality.ANOMALOUS || livenessAssessment.status == "ANOMALOUS"

        if (lowConversation && visualAnomalous) {
            return DisagreementResult(
                isDisagreement = true,
                explanation = "Visual fluctuations or anomalies were observed, but speech transcript shows no coercive scam tactics yet. Monitoring continues."
            )
        }

        return DisagreementResult(isDisagreement = false, explanation = null)
    }
}

/**
 * RiskFusionEngine: Combines conversation risk (primary), velocity, stage,
 * and visual/liveness signals (supporting) into an explainable assessment.
 */
class RiskFusionEngine {

    fun fuse(
        conversationScore: Int,
        signals: List<RiskSignal>,
        stage: ScamStage,
        velocity: VelocityCalculation,
        visualSignal: VisualSignal?,
        liveness: LivenessAssessment?,
        isSafetyBrakeTriggered: Boolean
    ): FusedRiskAssessment {
        val level = RiskLevel.fromScore(conversationScore)

        // Reasons breakdown
        val reasons = mutableListOf<String>()
        val groupedByTactic = signals.groupBy { it.tactic }
        for ((tactic, sigs) in groupedByTactic) {
            val totalWeight = sigs.sumOf { it.riskContribution }
            reasons.add("+$totalWeight ${tactic.displayName} (${sigs.size}x detected)")
        }

        // Supporting signals list
        val supporting = mutableListOf<String>()
        if (visualSignal != null) {
            supporting.add("Visual Stream: ${visualSignal.quality.name} (Face: ${if (visualSignal.faceDetected) "Localized" else "None"})")
        } else {
            supporting.add("Visual Stream: Disabled or No Camera Permission")
        }

        if (liveness != null) {
            supporting.add("Liveness Indicator: ${liveness.status} (${liveness.explanation})")
        }

        supporting.add("Velocity: ${velocity.level.name} (${velocity.explanation})")

        // Disagreement check
        val disagreement = ModelDisagreementGuard.checkDisagreement(level, visualSignal, liveness)

        // Confidence calculation
        val confidence = when {
            signals.size >= 4 -> 0.95f
            signals.size >= 2 -> 0.88f
            signals.isNotEmpty() -> 0.75f
            else -> 0.50f
        }

        return FusedRiskAssessment(
            overallRisk = conversationScore,
            riskLevel = level,
            scamStage = stage,
            manipulationVelocity = velocity.level,
            velocityExplanation = velocity.explanation,
            primarySignal = "Conversation Analysis (Deterministic local NLP)",
            supportingSignals = supporting,
            reasons = reasons,
            confidence = confidence,
            isSafetyBrakeRequired = isSafetyBrakeTriggered,
            disagreementDetected = disagreement.isDisagreement,
            disagreementExplanation = disagreement.explanation
        )
    }
}
