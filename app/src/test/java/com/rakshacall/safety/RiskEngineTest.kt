package com.rakshacall.safety

import com.rakshacall.safety.domain.engine.RiskEngine
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.TranscriptEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class RiskEngineTest {

    private lateinit var riskEngine: RiskEngine

    @Before
    fun setUp() {
        riskEngine = RiskEngine(lowThreshold = 30, highThreshold = 60, criticalThreshold = 80)
    }

    @Test
    fun `detects authority impersonation from live speech text`() {
        val event = TranscriptEvent(
            id = "t1",
            sessionId = "s1",
            text = "I am calling from Mumbai Police Cyber Crime Cell headquarters."
        )
        val signals = riskEngine.analyzeTranscript(event, emptyList())

        assertEquals(1, signals.size)
        assertEquals(ScamTactic.AUTHORITY_IMPERSONATION, signals[0].tactic)
        assertTrue(signals[0].riskContribution >= 15)
    }

    @Test
    fun `detects criminal allegation and fear tactics`() {
        val event = TranscriptEvent(
            id = "t2",
            sessionId = "s1",
            text = "Your Aadhaar is involved in illegal money laundering and narcotics contraband."
        )
        val signals = riskEngine.analyzeTranscript(event, emptyList())

        assertTrue(signals.any { it.tactic == ScamTactic.CRIMINAL_ALLEGATION })
    }

    @Test
    fun `detects isolation tactic`() {
        val event = TranscriptEvent(
            id = "t3",
            sessionId = "s1",
            text = "Do not disconnect this call. Stay in a quiet closed room and do not tell your family."
        )
        val signals = riskEngine.analyzeTranscript(event, emptyList())

        assertTrue(signals.any { it.tactic == ScamTactic.ISOLATION })
    }

    @Test
    fun `detects payment demand and irreversible action flag`() {
        val event = TranscriptEvent(
            id = "t4",
            sessionId = "s1",
            text = "Transfer 50,000 rupees immediately to RBI verification account for security deposit."
        )
        val signals = riskEngine.analyzeTranscript(event, emptyList())

        val paymentSig = signals.firstOrNull { it.tactic == ScamTactic.PAYMENT_DEMAND }
        assertTrue(paymentSig != null)
        assertTrue(paymentSig!!.tactic.isIrreversibleAction)
    }

    @Test
    fun `detects credential and OTP pressure`() {
        val event = TranscriptEvent(
            id = "t5",
            sessionId = "s1",
            text = "Give me your OTP right now to stop the arrest warrant."
        )
        val signals = riskEngine.analyzeTranscript(event, emptyList())

        assertTrue(signals.any { it.tactic == ScamTactic.CREDENTIAL_PRESSURE })
    }

    @Test
    fun `applies duplicate suppression dampening within 45 seconds`() {
        val now = System.currentTimeMillis()
        val event1 = TranscriptEvent(id = "e1", sessionId = "s1", timestamp = now, text = "Calling from CBI police headquarters.")
        val firstBatch = riskEngine.analyzeTranscript(event1, emptyList())
        val firstWeight = firstBatch[0].riskContribution

        // Repeated same tactic 10 seconds later
        val event2 = TranscriptEvent(id = "e2", sessionId = "s1", timestamp = now + 10_000L, text = "This is inspector from police department.")
        val secondBatch = riskEngine.analyzeTranscript(event2, firstBatch)
        val secondWeight = secondBatch[0].riskContribution

        // Second should be dampened (less weight than first)
        assertTrue("Expected second contribution ($secondWeight) to be less than first ($firstWeight)", secondWeight < firstWeight)
    }

    @Test
    fun `calculates cumulative score and escalation multiplier with multiple distinct tactics`() {
        val now = System.currentTimeMillis()
        val sig1 = RiskSignal(id = "1", timestamp = now, tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.9f, riskContribution = 15, evidenceText = "police", sessionId = "s1")
        val sig2 = RiskSignal(id = "2", timestamp = now + 5000L, tactic = ScamTactic.CRIMINAL_ALLEGATION, confidence = 0.9f, riskContribution = 15, evidenceText = "money laundering", sessionId = "s1")
        val sig3 = RiskSignal(id = "3", timestamp = now + 10000L, tactic = ScamTactic.ISOLATION, confidence = 0.9f, riskContribution = 15, evidenceText = "do not disconnect", sessionId = "s1")
        val sig4 = RiskSignal(id = "4", timestamp = now + 15000L, tactic = ScamTactic.PAYMENT_DEMAND, confidence = 0.9f, riskContribution = 20, evidenceText = "transfer ₹50000", sessionId = "s1")

        val score = riskEngine.calculateCurrentScore(listOf(sig1, sig2, sig3, sig4), sessionStartTime = now, currentTime = now + 20000L)

        // Raw sum is 65. With 4 distinct tactics, 1.35x escalation multiplier brings it to >= 80 (CRITICAL)
        assertTrue("Score ($score) should be >= 80 due to multi-tactic escalation", score >= 80)
        assertEquals(RiskLevel.CRITICAL, riskEngine.getRiskLevel(score))
    }

    @Test
    fun `triggers Safety Brake when score is HIGH and context includes payment pressure`() {
        val now = System.currentTimeMillis()
        val paymentSig = RiskSignal(id = "1", timestamp = now, tactic = ScamTactic.PAYMENT_DEMAND, confidence = 0.9f, riskContribution = 20, evidenceText = "transfer ₹50,000", sessionId = "s1")

        // Score 70 (HIGH) + Payment Demand -> Safety Brake MUST trigger
        assertTrue(riskEngine.isSafetyBrakeTriggered(70, listOf(paymentSig)))

        // Score 30 (MEDIUM) + Payment Demand -> Does not cross high threshold yet
        assertFalse(riskEngine.isSafetyBrakeTriggered(30, listOf(paymentSig)))

        // Score 70 (HIGH) with only non-irreversible tactic -> Does not trigger Safety Brake
        val authSig = RiskSignal(id = "2", timestamp = now, tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.9f, riskContribution = 15, evidenceText = "police", sessionId = "s1")
        assertFalse(riskEngine.isSafetyBrakeTriggered(70, listOf(authSig)))
    }
}
