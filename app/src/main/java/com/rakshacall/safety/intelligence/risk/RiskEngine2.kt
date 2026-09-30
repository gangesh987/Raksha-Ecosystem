package com.rakshacall.safety.intelligence.risk

import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.intelligence.behaviour.BehaviourMetrics
import com.rakshacall.safety.intelligence.intent.InferredIntent
import com.rakshacall.safety.protection.EvidenceStrength
import kotlin.math.roundToInt

data class SemanticRisk(
    val score: Int, // 0..100
    val tactics: List<ScamTactic>,
    val primaryIntent: InferredIntent
)

data class BehaviourRisk(
    val score: Int, // 0..100
    val metrics: BehaviourMetrics
)

data class VisualRisk(
    val score: Int, // 0..100
    val threatCount: Int,
    val description: String? = null
)

data class AudioRisk(
    val score: Int, // 0..100
    val speechRateWpm: Int = 140,
    val vocalStressLevel: Float = 0.0f
)

data class ContextRisk(
    val score: Int, // 0..100
    val isUnsavedNumber: Boolean = true,
    val callerClaimVsHistory: Float = 0.0f
)

data class StageRisk(
    val score: Int, // 0..100
    val stage: ScamStage
)

data class FusedRiskResult(
    val rawRisk: Int,
    val smoothedRisk: Int,
    val confidence: Float,
    val evidenceStrength: EvidenceStrength,
    val riskLevel: RiskLevel,
    val reasons: List<String>,
    val components: Map<String, Int>
)

/**
 * Risk Engine 2.0 (Phase 11).
 * Decomposes multi-modal risk into six independent components and fuses them
 * via multi-criteria weighting, temporal smoothing, and hysteresis.
 */
class RiskFusionEngine2(
    private val smoothingFactor: Float = 0.35f
) {
    private var previousSmoothedRisk: Float = 0.0f
    private var previousRiskLevel: RiskLevel = RiskLevel.LOW

    fun fuse(
        semantic: SemanticRisk,
        behaviour: BehaviourRisk,
        visual: VisualRisk,
        audio: AudioRisk,
        context: ContextRisk,
        stage: StageRisk,
        confidenceAssessment: ConfidenceAssessment
    ): FusedRiskResult {
        // Safe phrase veto: if intent is explicitly safe/defensive, suppress coercive risk
        if (semantic.primaryIntent == InferredIntent.SAFE_ADVICE_OR_DEFENSE) {
            previousSmoothedRisk = 0.0f
            previousRiskLevel = RiskLevel.LOW
            return FusedRiskResult(
                rawRisk = 0,
                smoothedRisk = 0,
                confidence = confidenceAssessment.score,
                evidenceStrength = EvidenceStrength.WEAK,
                riskLevel = RiskLevel.LOW,
                reasons = listOf("Safe defensive/educational advice detected. Risk suppressed."),
                components = mapOf(
                    "Semantic" to 0,
                    "Behaviour" to 0,
                    "Visual" to visual.score,
                    "Audio" to audio.score,
                    "Context" to context.score,
                    "Stage" to stage.score
                )
            )
        }

        // Weighted raw risk formulation
        // Semantic: 40%, Behaviour: 25%, Visual: 15%, Stage: 10%, Audio: 5%, Context: 5%
        val rawWeighted = (
                semantic.score * 0.40f +
                behaviour.score * 0.25f +
                visual.score * 0.15f +
                stage.score * 0.10f +
                audio.score * 0.05f +
                context.score * 0.05f
        ).coerceIn(0f, 100f)

        val hasIrreversibleDemand = semantic.tactics.any { it.isIrreversibleAction } ||
                semantic.primaryIntent == InferredIntent.POSSIBLE_VERIFICATION_CODE_REQUEST ||
                semantic.primaryIntent == InferredIntent.POSSIBLE_FINANCIAL_TRANSFER_REQUEST ||
                semantic.primaryIntent == InferredIntent.POSSIBLE_REMOTE_ACCESS_REQUEST

        val effectiveRaw = if (hasIrreversibleDemand && semantic.score >= 60) {
            maxOf(rawWeighted, semantic.score.toFloat())
        } else {
            rawWeighted
        }

        val rawInt = effectiveRaw.roundToInt()

        // Temporal exponential moving average smoothing (fast-tracked on irreversible demands)
        val smoothedFloat = if (previousSmoothedRisk == 0f || (hasIrreversibleDemand && semantic.score >= 75)) {
            effectiveRaw
        } else {
            (previousSmoothedRisk * (1f - smoothingFactor)) + (effectiveRaw * smoothingFactor)
        }
        previousSmoothedRisk = smoothedFloat
        val smoothedInt = smoothedFloat.roundToInt()

        // Hysteresis for RiskLevel stability (prevent flickering at boundaries)
        val candidateLevel = when {
            smoothedInt >= 80 -> RiskLevel.CRITICAL
            smoothedInt >= 60 -> RiskLevel.HIGH
            smoothedInt >= 35 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        // Apply hysteresis: only step down if smoothed risk drops 5 points below boundary
        val stableLevel = if (candidateLevel.ordinal < previousRiskLevel.ordinal) {
            val threshold = when (previousRiskLevel) {
                RiskLevel.CRITICAL -> 75
                RiskLevel.HIGH -> 55
                RiskLevel.MEDIUM -> 30
                RiskLevel.LOW -> 0
            }
            if (smoothedInt < threshold) candidateLevel else previousRiskLevel
        } else {
            candidateLevel
        }
        previousRiskLevel = stableLevel

        // Explanatory breakdown
        val reasons = mutableListOf<String>()
        if (semantic.score > 0) {
            reasons.add("Semantic: ${semantic.primaryIntent.name} (${semantic.tactics.joinToString { it.displayName }})")
        }
        if (behaviour.score > 30) {
            reasons.add("Behaviour: Pressure velocity ${(behaviour.metrics.manipulationVelocity * 100).toInt()}% / control ${(behaviour.metrics.conversationControl * 100).toInt()}%")
        }
        if (visual.score > 0) {
            reasons.add("Visual: ${visual.threatCount} threat signals (${visual.description ?: "interface anomaly"})")
        }
        if (stage.score > 40) {
            reasons.add("Stage: ${stage.stage.displayName}")
        }

        return FusedRiskResult(
            rawRisk = rawInt,
            smoothedRisk = smoothedInt,
            confidence = confidenceAssessment.score,
            evidenceStrength = confidenceAssessment.evidenceStrength,
            riskLevel = stableLevel,
            reasons = reasons,
            components = mapOf(
                "Semantic" to semantic.score,
                "Behaviour" to behaviour.score,
                "Visual" to visual.score,
                "Audio" to audio.score,
                "Context" to context.score,
                "Stage" to stage.score
            )
        )
    }

    fun reset() {
        previousSmoothedRisk = 0.0f
        previousRiskLevel = RiskLevel.LOW
    }
}
