package com.rakshacall.safety

import com.rakshacall.safety.domain.engine.ManipulationVelocityEngine
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.VelocityLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ManipulationVelocityTest {

    private val engine = ManipulationVelocityEngine(windowMillis = 90_000L)

    @Test
    fun `rapid barrage of coercive tactics within 30 seconds triggers HIGH velocity`() {
        val now = 100_000L
        val signals = listOf(
            RiskSignal(id = "1", timestamp = now - 25_000L, tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.9f, riskContribution = 15, evidenceText = "police", sessionId = "s1"),
            RiskSignal(id = "2", timestamp = now - 15_000L, tactic = ScamTactic.CRIMINAL_ALLEGATION, confidence = 0.9f, riskContribution = 15, evidenceText = "drugs found", sessionId = "s1"),
            RiskSignal(id = "3", timestamp = now - 5_000L, tactic = ScamTactic.ISOLATION, confidence = 0.9f, riskContribution = 15, evidenceText = "do not disconnect", sessionId = "s1")
        )

        val result = engine.calculateVelocity(signals, currentTime = now)

        assertEquals(VelocityLevel.HIGH, result.level)
        assertEquals(3, result.tacticCountInWindow)
    }

    @Test
    fun `sparse tactics across wide time produce LOW velocity`() {
        val now = 200_000L
        // 1 tactic in window, other is outside 90s window
        val signals = listOf(
            RiskSignal(id = "1", timestamp = now - 150_000L, tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.9f, riskContribution = 15, evidenceText = "police", sessionId = "s1"),
            RiskSignal(id = "2", timestamp = now - 20_000L, tactic = ScamTactic.URGENCY, confidence = 0.9f, riskContribution = 10, evidenceText = "immediately", sessionId = "s1")
        )

        val result = engine.calculateVelocity(signals, currentTime = now)

        assertEquals(VelocityLevel.LOW, result.level)
        assertEquals(1, result.tacticCountInWindow)
    }
}
