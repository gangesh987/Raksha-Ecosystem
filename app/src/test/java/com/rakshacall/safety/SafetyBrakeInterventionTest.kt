package com.rakshacall.safety

import com.rakshacall.safety.domain.engine.RiskEngine
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamTactic
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SafetyBrakeInterventionTest {

    private lateinit var riskEngine: RiskEngine

    @Before
    fun setUp() {
        riskEngine = RiskEngine(lowThreshold = 30, highThreshold = 60, criticalThreshold = 80)
    }

    private fun createSignal(tactic: ScamTactic, contribution: Int): RiskSignal {
        return RiskSignal(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            tactic = tactic,
            confidence = 0.95f,
            riskContribution = contribution,
            evidenceText = "Test evidence for ${tactic.name}",
            sessionId = "test-session"
        )
    }

    @Test
    fun testSafetyBrakeTriggersOnHighRiskWithPaymentDemand() {
        val signals = listOf(
            createSignal(ScamTactic.AUTHORITY_IMPERSONATION, 15),
            createSignal(ScamTactic.CRIMINAL_ALLEGATION, 15),
            createSignal(ScamTactic.ISOLATION, 15),
            createSignal(ScamTactic.PAYMENT_DEMAND, 20)
        )
        val score = 65
        val triggered = riskEngine.isSafetyBrakeTriggered(score, signals)
        assertTrue("Safety Brake must trigger when score >= 60 AND Payment Demand is present", triggered)
    }

    @Test
    fun testSafetyBrakeTriggersOnHighRiskWithCredentialPressure() {
        val signals = listOf(
            createSignal(ScamTactic.AUTHORITY_IMPERSONATION, 15),
            createSignal(ScamTactic.CRIMINAL_ALLEGATION, 15),
            createSignal(ScamTactic.CREDENTIAL_PRESSURE, 20)
        )
        val score = 70
        val triggered = riskEngine.isSafetyBrakeTriggered(score, signals)
        assertTrue("Safety Brake must trigger when score >= 60 AND Credential Pressure is present", triggered)
    }

    @Test
    fun testSafetyBrakeTriggersOnHighRiskWithRemoteAccess() {
        val signals = listOf(
            createSignal(ScamTactic.AUTHORITY_IMPERSONATION, 15),
            createSignal(ScamTactic.REMOTE_ACCESS, 15)
        )
        val score = 62
        val triggered = riskEngine.isSafetyBrakeTriggered(score, signals)
        assertTrue("Safety Brake must trigger when score >= 60 AND Remote Access is present", triggered)
    }

    @Test
    fun testSafetyBrakeDoesNotTriggerWhenRiskLowEvenWithPayment() {
        val signals = listOf(
            createSignal(ScamTactic.PAYMENT_DEMAND, 20)
        )
        val score = 25 // Below threshold 60
        val triggered = riskEngine.isSafetyBrakeTriggered(score, signals)
        assertFalse("Safety Brake must NOT trigger when score < 60, even if Payment is mentioned", triggered)
    }

    @Test
    fun testSafetyBrakeDoesNotTriggerWhenRiskHighWithoutIrreversibleTactic() {
        // High score (65) from non-irreversible tactics (Authority + Fear + Urgency + Isolation)
        val signals = listOf(
            createSignal(ScamTactic.AUTHORITY_IMPERSONATION, 15),
            createSignal(ScamTactic.CRIMINAL_ALLEGATION, 15),
            createSignal(ScamTactic.URGENCY, 10),
            createSignal(ScamTactic.ISOLATION, 15),
            createSignal(ScamTactic.ESCALATION, 10)
        )
        val score = 65
        val triggered = riskEngine.isSafetyBrakeTriggered(score, signals)
        assertFalse("Safety Brake must NOT trigger if irreversible tactic (Payment/OTP/Remote) is not yet detected", triggered)
    }

    @Test
    fun testIrreversibleTacticHelperCheck() {
        assertTrue(ScamTactic.PAYMENT_DEMAND.isIrreversibleAction)
        assertTrue(ScamTactic.CREDENTIAL_PRESSURE.isIrreversibleAction)
        assertTrue(ScamTactic.REMOTE_ACCESS.isIrreversibleAction)

        assertFalse(ScamTactic.AUTHORITY_IMPERSONATION.isIrreversibleAction)
        assertFalse(ScamTactic.CRIMINAL_ALLEGATION.isIrreversibleAction)
        assertFalse(ScamTactic.URGENCY.isIrreversibleAction)
        assertFalse(ScamTactic.ISOLATION.isIrreversibleAction)
        assertFalse(ScamTactic.SUSPICIOUS_LINK.isIrreversibleAction)
        assertFalse(ScamTactic.ESCALATION.isIrreversibleAction)
    }
}
