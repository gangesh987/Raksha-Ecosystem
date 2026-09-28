package com.rakshacall.safety.domain.engine

import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.VelocityLevel

data class VelocityCalculation(
    val level: VelocityLevel,
    val tacticCountInWindow: Int,
    val windowDurationSeconds: Long,
    val weightedScoreInWindow: Int,
    val explanation: String
)

/**
 * Calculates Manipulation Velocity over actual event timestamps.
 * Measures tactic frequency, severity, and temporal proximity.
 */
class ManipulationVelocityEngine(
    private val windowMillis: Long = 90_000L // 90-second sliding analysis window
) {

    fun calculateVelocity(
        signals: List<RiskSignal>,
        currentTime: Long = System.currentTimeMillis()
    ): VelocityCalculation {
        if (signals.isEmpty()) {
            return VelocityCalculation(
                level = VelocityLevel.LOW,
                tacticCountInWindow = 0,
                windowDurationSeconds = windowMillis / 1000L,
                weightedScoreInWindow = 0,
                explanation = "No tactics detected."
            )
        }

        // Filter signals strictly within the recent sliding window
        val windowStart = currentTime - windowMillis
        val signalsInWindow = signals.filter { it.timestamp >= windowStart }

        val count = signalsInWindow.size
        val weightedScore = signalsInWindow.sumOf { it.riskContribution }

        // Calculate average interval between events in window
        val intervalSeconds = if (count > 1) {
            val minTs = signalsInWindow.minOf { it.timestamp }
            val maxTs = signalsInWindow.maxOf { it.timestamp }
            val span = (maxTs - minTs) / 1000L
            span / (count - 1).coerceAtLeast(1)
        } else {
            90L
        }

        val level = when {
            count >= 3 && intervalSeconds <= 30L || weightedScore >= 40 -> VelocityLevel.HIGH
            count >= 2 || weightedScore >= 25 -> VelocityLevel.MODERATE
            else -> VelocityLevel.LOW
        }

        val explanation = when (level) {
            VelocityLevel.HIGH -> "$count coercive tactics detected within $intervalSeconds-second intervals (rapid coercion spike)."
            VelocityLevel.MODERATE -> "$count coercive tactics detected recently. Coercion is actively escalating."
            VelocityLevel.LOW -> if (count == 0) "Tactics observed with substantial intervals." else "1 tactic detected in recent window."
        }

        return VelocityCalculation(
            level = level,
            tacticCountInWindow = count,
            windowDurationSeconds = windowMillis / 1000L,
            weightedScoreInWindow = weightedScore,
            explanation = explanation
        )
    }
}
