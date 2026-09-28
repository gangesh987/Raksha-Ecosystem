package com.rakshacall.safety

import com.rakshacall.safety.domain.engine.ScamStageMachine
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ScamStageMachineTest {

    private lateinit var stageMachine: ScamStageMachine

    @Before
    fun setUp() {
        stageMachine = ScamStageMachine()
    }

    @Test
    fun `initial stage starts at CONTACT`() {
        assertEquals(ScamStage.CONTACT, stageMachine.getCurrentStage())
    }

    @Test
    fun `progresses sequentially through AUTHORITY and FEAR upon verified tactics`() {
        val now = System.currentTimeMillis()
        val authSig = RiskSignal(id = "1", timestamp = now, tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.9f, riskContribution = 15, evidenceText = "police", sessionId = "s1")
        val t1 = stageMachine.processSignal(authSig)

        assertNotNull(t1)
        assertEquals(ScamStage.CONTACT, t1!!.fromStage)
        assertEquals(ScamStage.AUTHORITY, t1.toStage)
        assertEquals(ScamStage.AUTHORITY, stageMachine.getCurrentStage())

        val fearSig = RiskSignal(id = "2", timestamp = now + 1000L, tactic = ScamTactic.CRIMINAL_ALLEGATION, confidence = 0.9f, riskContribution = 15, evidenceText = "money laundering", sessionId = "s1")
        val t2 = stageMachine.processSignal(fearSig)

        assertNotNull(t2)
        assertEquals(ScamStage.AUTHORITY, t2!!.fromStage)
        assertEquals(ScamStage.FEAR, t2.toStage)
        assertEquals(ScamStage.FEAR, stageMachine.getCurrentStage())
    }

    @Test
    fun `strictly preserves monotonic progression and does not regress to earlier stage`() {
        val now = System.currentTimeMillis()
        // Advance to PAYMENT_CREDENTIAL
        val paymentSig = RiskSignal(id = "1", timestamp = now, tactic = ScamTactic.PAYMENT_DEMAND, confidence = 0.9f, riskContribution = 20, evidenceText = "transfer ₹50000", sessionId = "s1")
        stageMachine.processSignal(paymentSig)
        assertEquals(ScamStage.PAYMENT_CREDENTIAL, stageMachine.getCurrentStage())

        // An earlier stage tactic (e.g. Authority) arrives later in conversation
        val authSig = RiskSignal(id = "2", timestamp = now + 5000L, tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.9f, riskContribution = 15, evidenceText = "police", sessionId = "s1")
        val t = stageMachine.processSignal(authSig)

        // Stage must NOT regress back to AUTHORITY
        assertNull(t)
        assertEquals(ScamStage.PAYMENT_CREDENTIAL, stageMachine.getCurrentStage())
    }
}
