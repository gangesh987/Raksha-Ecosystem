package com.rakshacall.safety

import com.rakshacall.safety.domain.engine.RiskEngine
import com.rakshacall.safety.domain.model.ScamTactic
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ScamTacticCategoriesTest {

    private lateinit var riskEngine: RiskEngine

    @Before
    fun setUp() {
        riskEngine = RiskEngine()
    }

    @Test
    fun testTactic1AuthorityImpersonation() {
        val text = "I am calling from the Central Bureau of Investigation and Mumbai Police cyber cell."
        val signals = riskEngine.analyzeTranscript(text)
        assertTrue(signals.any { it.tactic == ScamTactic.AUTHORITY_IMPERSONATION })
        assertEquals(15, ScamTactic.AUTHORITY_IMPERSONATION.defaultWeight)
    }

    @Test
    fun testTactic2CriminalAllegationAndFear() {
        val text = "Your Aadhaar is misused in a money laundering crime and an arrest warrant has been issued."
        val signals = riskEngine.analyzeTranscript(text)
        assertTrue(signals.any { it.tactic == ScamTactic.CRIMINAL_ALLEGATION })
        assertEquals(15, ScamTactic.CRIMINAL_ALLEGATION.defaultWeight)
    }

    @Test
    fun testTactic3Urgency() {
        val text = "You must resolve this immediately right now within 10 minutes without delay."
        val signals = riskEngine.analyzeTranscript(text)
        assertTrue(signals.any { it.tactic == ScamTactic.URGENCY })
        assertEquals(10, ScamTactic.URGENCY.defaultWeight)
    }

    @Test
    fun testTactic4Isolation() {
        val text = "Do not disconnect this call, stay in a closed room and do not tell your family."
        val signals = riskEngine.analyzeTranscript(text)
        assertTrue(signals.any { it.tactic == ScamTactic.ISOLATION })
        assertEquals(15, ScamTactic.ISOLATION.defaultWeight)
    }

    @Test
    fun testTactic5PaymentDemand() {
        val text = "You must transfer the money to this RBI security deposit verification account immediately."
        val signals = riskEngine.analyzeTranscript(text)
        assertTrue(signals.any { it.tactic == ScamTactic.PAYMENT_DEMAND })
        assertEquals(20, ScamTactic.PAYMENT_DEMAND.defaultWeight)
    }

    @Test
    fun testTactic6CredentialPressure() {
        val text = "Give me your OTP right now and enter your UPI PIN on the screen."
        val signals = riskEngine.analyzeTranscript(text)
        assertTrue(signals.any { it.tactic == ScamTactic.CREDENTIAL_PRESSURE })
        assertEquals(20, ScamTactic.CREDENTIAL_PRESSURE.defaultWeight)
    }

    @Test
    fun testTactic7RemoteAccessPressure() {
        val text = "Download and install AnyDesk or TeamViewer and share your screen with our team."
        val signals = riskEngine.analyzeTranscript(text)
        assertTrue(signals.any { it.tactic == ScamTactic.REMOTE_ACCESS })
        assertEquals(15, ScamTactic.REMOTE_ACCESS.defaultWeight)
    }

    @Test
    fun testTactic8SuspiciousLinks() {
        val text = "Click on the link https://verification-portal.xyz/verify and download the apk file."
        val signals = riskEngine.analyzeTranscript(text)
        assertTrue(signals.any { it.tactic == ScamTactic.SUSPICIOUS_LINK })
        assertEquals(10, ScamTactic.SUSPICIOUS_LINK.defaultWeight)
    }

    @Test
    fun testTactic9CoerciveEscalation() {
        val text = "We will send a police team to your house and freeze all your bank accounts today."
        val signals = riskEngine.analyzeTranscript(text)
        assertTrue(signals.any { it.tactic == ScamTactic.ESCALATION })
        assertEquals(10, ScamTactic.ESCALATION.defaultWeight)
    }
}
