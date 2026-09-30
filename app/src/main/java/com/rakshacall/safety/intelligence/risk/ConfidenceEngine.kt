package com.rakshacall.safety.intelligence.risk

import com.rakshacall.safety.protection.EvidenceStrength

data class ConfidenceAssessment(
    val score: Float,              // 0.0 .. 1.0 (e.g. 0.91 = 91%)
    val evidenceStrength: EvidenceStrength,
    val corroboratingSourcesCount: Int,
    val explanation: String
)

/**
 * Orthogonal Confidence Engine (Phase 12).
 * Strictly calculates confidence separately from risk magnitude.
 * Evaluates source diversity, graph connectivity, acoustic clarity, and multi-turn stability.
 */
class ConfidenceEngine {

    fun evaluateConfidence(
        asrConfidence: Float,
        tacticsCount: Int,
        distinctSourceCount: Int,
        graphCorroborationScore: Float,
        hasVisualCorroboration: Boolean,
        isHardNegativeSafe: Boolean
    ): ConfidenceAssessment {
        if (isHardNegativeSafe) {
            return ConfidenceAssessment(
                score = 0.98f,
                evidenceStrength = EvidenceStrength.WEAK,
                corroboratingSourcesCount = 1,
                explanation = "High confidence in safe/defensive negative determination."
            )
        }

        var sourceScore = (distinctSourceCount.toFloat() / 3f).coerceIn(0.2f, 1.0f)
        if (hasVisualCorroboration) sourceScore = (sourceScore + 0.2f).coerceAtMost(1.0f)

        val tacticScore = (tacticsCount.toFloat() / 4f).coerceIn(0.1f, 1.0f)

        // Weighted orthogonal confidence calculation
        val combinedConfidence = (
                asrConfidence * 0.25f +
                sourceScore * 0.35f +
                tacticScore * 0.20f +
                graphCorroborationScore * 0.20f
        ).coerceIn(0.1f, 0.99f)

        // Evidence Strength Tiers (Phase 19 & Phase 12)
        val strength = when {
            distinctSourceCount >= 3 && combinedConfidence >= 0.85f && tacticsCount >= 3 -> EvidenceStrength.VERY_STRONG
            distinctSourceCount >= 2 && tacticsCount >= 2 -> EvidenceStrength.STRONG
            tacticsCount >= 1 -> EvidenceStrength.MODERATE
            else -> EvidenceStrength.WEAK
        }

        val explanation = when (strength) {
            EvidenceStrength.VERY_STRONG -> "Multi-source corroboration with high acoustic and tactical clarity ($distinctSourceCount independent channels)."
            EvidenceStrength.STRONG -> "Corroborated evidence from multiple independent signals."
            EvidenceStrength.MODERATE -> "Single-channel tactical observation without independent corroboration."
            EvidenceStrength.WEAK -> "Low-confidence or uncorroborated ambient signal."
        }

        return ConfidenceAssessment(
            score = combinedConfidence,
            evidenceStrength = strength,
            corroboratingSourcesCount = distinctSourceCount,
            explanation = explanation
        )
    }
}
