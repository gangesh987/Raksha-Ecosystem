package com.rakshacall.safety

import com.rakshacall.safety.domain.engine.LivenessAssessment
import com.rakshacall.safety.domain.engine.ModelDisagreementGuard
import com.rakshacall.safety.domain.engine.RiskFusionEngine
import com.rakshacall.safety.domain.engine.VelocityCalculation
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.VelocityLevel
import com.rakshacall.safety.domain.model.VisualQuality
import com.rakshacall.safety.domain.model.VisualSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskFusionEngineTest {

    private val fusionEngine = RiskFusionEngine()

    @Test
    fun `fuses signals with conversation as primary and includes explicit reasons`() {
        val now = System.currentTimeMillis()
        val sig1 = RiskSignal(id = "1", timestamp = now, tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.9f, riskContribution = 15, evidenceText = "police", sessionId = "s1")
        val sig2 = RiskSignal(id = "2", timestamp = now, tactic = ScamTactic.PAYMENT_DEMAND, confidence = 0.9f, riskContribution = 20, evidenceText = "transfer ₹50000", sessionId = "s1")

        val velocity = VelocityCalculation(
            level = VelocityLevel.MODERATE,
            tacticCountInWindow = 2,
            windowDurationSeconds = 90L,
            weightedScoreInWindow = 35,
            explanation = "2 coercive tactics detected recently."
        )

        val assessment = fusionEngine.fuse(
            conversationScore = 75,
            signals = listOf(sig1, sig2),
            stage = ScamStage.PAYMENT_CREDENTIAL,
            velocity = velocity,
            visualSignal = null,
            liveness = null,
            isSafetyBrakeTriggered = true
        )

        assertEquals(RiskLevel.HIGH, assessment.riskLevel)
        assertEquals(75, assessment.overallRisk)
        assertTrue(assessment.isSafetyBrakeRequired)
        assertTrue(assessment.primarySignal.contains("Conversation"))
        assertTrue(assessment.reasons.any { it.contains("Authority Impersonation") })
        assertTrue(assessment.reasons.any { it.contains("Payment Demand") })
    }

    @Test
    fun `detects model disagreement when conversation is high but visual is clear`() {
        val visual = VisualSignal(
            quality = VisualQuality.CLEAR,
            faceDetected = true,
            faceStabilityScore = 0.9f,
            lightingScore = 0.85f,
            confidence = 0.85f
        )
        val liveness = LivenessAssessment(
            status = "CLEAR",
            score = 0.9f,
            confidence = 0.8f,
            explanation = "Natural lighting"
        )

        val result = ModelDisagreementGuard.checkDisagreement(
            conversationRiskLevel = RiskLevel.HIGH,
            visualSignal = visual,
            livenessAssessment = liveness
        )

        assertTrue(result.isDisagreement)
        assertNotNull(result.explanation)
        assertTrue(result.explanation!!.contains("conversational coercion remains PRIMARY"))
    }
}
