package com.rakshacall.safety.intelligence.router

enum class ModelExecutionTier {
    FAST_LOCAL,
    ADVANCED_LOCAL,
    CLOUD,
    RULE_FALLBACK
}

enum class DegradationStatus {
    LOCAL,
    CLOUD,
    HYBRID,
    DEGRADED
}

data class RoutingDecision(
    val activeTier: ModelExecutionTier,
    val degradationStatus: DegradationStatus,
    val reason: String
)

data class ModelDisagreementResult(
    val hasDisagreement: Boolean,
    val agreementRatio: Float,
    val combinedConfidence: Float,
    val resolvedRiskScore: Int,
    val explanation: String
)

/**
 * Model Router & Disagreement Engine (Phases 22, 23, 24).
 * Selects the optimal execution tier based on connectivity, device capability,
 * and privacy settings; transparently arbitrates disagreements between models.
 */
class ModelRouter {

    fun route(
        isNetworkAvailable: Boolean,
        isCloudAuthorized: Boolean,
        localModelLoaded: Boolean,
        averageLatencyMs: Long
    ): RoutingDecision {
        return when {
            isCloudAuthorized && isNetworkAvailable && averageLatencyMs < 600L -> {
                RoutingDecision(
                    activeTier = ModelExecutionTier.CLOUD,
                    degradationStatus = DegradationStatus.HYBRID,
                    reason = "Cloud model active for rich multilingual analysis with local fallback."
                )
            }
            localModelLoaded && averageLatencyMs < 200L -> {
                RoutingDecision(
                    activeTier = ModelExecutionTier.ADVANCED_LOCAL,
                    degradationStatus = DegradationStatus.LOCAL,
                    reason = "On-device neural inference active under zero-retention privacy policy."
                )
            }
            localModelLoaded -> {
                RoutingDecision(
                    activeTier = ModelExecutionTier.FAST_LOCAL,
                    degradationStatus = DegradationStatus.LOCAL,
                    reason = "On-device fast semantic parser active."
                )
            }
            else -> {
                RoutingDecision(
                    activeTier = ModelExecutionTier.RULE_FALLBACK,
                    degradationStatus = DegradationStatus.DEGRADED,
                    reason = "Deterministic safety rules active due to resource/network constraints."
                )
            }
        }
    }

    /**
     * Resolve divergence when two independent models evaluate the same transcript (Phase 23).
     */
    fun arbitrate(
        modelAScore: Int,
        modelAConfidence: Float,
        modelBScore: Int,
        modelBConfidence: Float,
        hasSupportingVisualThreat: Boolean
    ): ModelDisagreementResult {
        val diff = kotlin.math.abs(modelAScore - modelBScore)
        val hasDisagreement = diff >= 30

        if (!hasDisagreement) {
            val agreement = 1.0f - (diff.toFloat() / 100f)
            val blendedScore = ((modelAScore * modelAConfidence + modelBScore * modelBConfidence) / (modelAConfidence + modelBConfidence)).toInt()
            val combinedConf = ((modelAConfidence + modelBConfidence) / 2f)

            return ModelDisagreementResult(
                hasDisagreement = false,
                agreementRatio = agreement,
                combinedConfidence = combinedConf,
                resolvedRiskScore = blendedScore,
                explanation = "Models in consensus (agreement ${(agreement * 100).toInt()}%)."
            )
        }

        // Significant disagreement detected: Apply Safety-First Principle
        // If one model flags high risk and there is supporting visual threat, adopt higher risk.
        val maxScore = maxOf(modelAScore, modelBScore)
        val minScore = minOf(modelAScore, modelBScore)

        val resolvedScore = if (hasSupportingVisualThreat || maxScore >= 80) {
            maxScore
        } else {
            // Blended conservative score without visual corroboration
            ((maxScore * 0.65f) + (minScore * 0.35f)).toInt()
        }

        val penalizedConfidence = ((modelAConfidence + modelBConfidence) / 2f) * 0.75f // Confidence penalty for disagreement

        return ModelDisagreementResult(
            hasDisagreement = true,
            agreementRatio = 1.0f - (diff.toFloat() / 100f),
            combinedConfidence = penalizedConfidence,
            resolvedRiskScore = resolvedScore,
            explanation = "Disagreement detected (Model A: $modelAScore, Model B: $modelBScore). Safety-first arbitration applied with confidence penalty."
        )
    }
}
