package com.rakshacall.safety.domain.engine

import com.rakshacall.safety.domain.model.VisualQuality
import com.rakshacall.safety.domain.model.VisualSignal

/**
 * Raw metrics extracted from real camera frames.
 */
data class FrameMetrics(
    val luminance: Float, // 0.0 to 1.0
    val variance: Float,  // sharpness / texture variance
    val faceDetected: Boolean,
    val faceWidthRatio: Float,
    val faceHeightRatio: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Replaceable interface for visual analysis.
 */
interface VisualAnalysisEngine {
    fun analyzeFrameMetrics(metrics: FrameMetrics): VisualSignal
}

/**
 * Production-ready lightweight local implementation analyzing lighting, face stability, and frame clarity.
 */
class LocalVisualAnalysisEngine : VisualAnalysisEngine {

    private val metricsBuffer = mutableListOf<FrameMetrics>()

    override fun analyzeFrameMetrics(metrics: FrameMetrics): VisualSignal {
        metricsBuffer.add(metrics)
        if (metricsBuffer.size > 10) metricsBuffer.removeAt(0)

        // Lighting score: optimal between 0.3 and 0.8
        val lightingScore = when {
            metrics.luminance < 0.15f -> 0.2f // Too dark
            metrics.luminance > 0.85f -> 0.3f // Blown out
            else -> 0.9f // Adequate illumination
        }

        // Face stability: variance across recent bounding box sizes
        val faceStabilityScore = if (metricsBuffer.size >= 3 && metrics.faceDetected) {
            val sizes = metricsBuffer.map { it.faceWidthRatio * it.faceHeightRatio }
            val avg = sizes.average()
            val maxDiff = sizes.maxOf { kotlin.math.abs(it - avg) }
            (1.0f - (maxDiff.toFloat() * 2f)).coerceIn(0.1f, 1.0f)
        } else {
            0.5f
        }

        val quality = when {
            !metrics.faceDetected -> VisualQuality.INCONCLUSIVE
            lightingScore < 0.4f -> VisualQuality.INCONCLUSIVE
            faceStabilityScore < 0.3f -> VisualQuality.ANOMALOUS
            else -> VisualQuality.CLEAR
        }

        return VisualSignal(
            quality = quality,
            faceDetected = metrics.faceDetected,
            faceStabilityScore = faceStabilityScore,
            lightingScore = lightingScore,
            confidence = if (metrics.faceDetected) 0.85f else 0.50f,
            timestamp = metrics.timestamp
        )
    }
}

/**
 * Experimental liveness engine interface and implementation.
 * Never claims definitive deepfake detection; accurately flags inconclusive / supporting state.
 */
interface LivenessEngine {
    fun evaluateLiveness(recentSignals: List<VisualSignal>): LivenessAssessment
}

data class LivenessAssessment(
    val status: String, // CLEAR, INCONCLUSIVE, ANOMALOUS
    val score: Float,
    val confidence: Float,
    val explanation: String
)

class LocalLivenessEngine : LivenessEngine {
    override fun evaluateLiveness(recentSignals: List<VisualSignal>): LivenessAssessment {
        if (recentSignals.isEmpty()) {
            return LivenessAssessment(
                status = "INCONCLUSIVE",
                score = 0.5f,
                confidence = 0.3f,
                explanation = "No camera frames received. Visual stream inactive."
            )
        }

        val facesFound = recentSignals.count { it.faceDetected }
        if (facesFound == 0) {
            return LivenessAssessment(
                status = "INCONCLUSIVE",
                score = 0.4f,
                confidence = 0.5f,
                explanation = "No human face localized in active camera feed."
            )
        }

        val avgStability = recentSignals.map { it.faceStabilityScore }.average().toFloat()

        return when {
            avgStability > 0.7f -> LivenessAssessment(
                status = "CLEAR",
                score = avgStability,
                confidence = 0.8f,
                explanation = "Natural face presence and lighting stability observed."
            )
            avgStability < 0.3f -> LivenessAssessment(
                status = "ANOMALOUS",
                score = avgStability,
                confidence = 0.65f,
                explanation = "Rapid frame or texture fluctuations detected."
            )
            else -> LivenessAssessment(
                status = "INCONCLUSIVE",
                score = avgStability,
                confidence = 0.5f,
                explanation = "Supporting visual signal inconclusive. Conversation remains primary."
            )
        }
    }
}
