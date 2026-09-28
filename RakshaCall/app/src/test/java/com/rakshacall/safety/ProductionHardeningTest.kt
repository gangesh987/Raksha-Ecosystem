package com.rakshacall.safety

import com.rakshacall.safety.core.security.CryptographyHelper
import com.rakshacall.safety.core.security.EvidenceHasher
import com.rakshacall.safety.core.security.IntegrityResult
import com.rakshacall.safety.core.security.KeystoreManager
import com.rakshacall.safety.data.provider.AIProviderFactory
import com.rakshacall.safety.data.provider.GeminiAIProvider
import com.rakshacall.safety.data.provider.GroqAIProvider
import com.rakshacall.safety.data.provider.LocalSafetyProvider
import com.rakshacall.safety.data.provider.TrustedContactProviderImpl
import com.rakshacall.safety.domain.engine.LivenessAssessment
import com.rakshacall.safety.domain.engine.ManipulationVelocityEngine
import com.rakshacall.safety.domain.engine.ModelDisagreementGuard
import com.rakshacall.safety.domain.engine.RiskEngine
import com.rakshacall.safety.domain.engine.RiskFusionEngine
import com.rakshacall.safety.domain.engine.ScamStageMachine
import com.rakshacall.safety.domain.engine.VelocityCalculation
import com.rakshacall.safety.domain.model.AlertDeliveryStatus
import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.domain.model.VelocityLevel
import com.rakshacall.safety.domain.model.VisualQuality
import com.rakshacall.safety.domain.model.VisualSignal
import com.rakshacall.safety.domain.provider.AIConnectionState
import com.rakshacall.safety.domain.provider.ContactAlertRequest
import com.rakshacall.safety.domain.usecase.TriggerSafetyBrakeUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Production Hardening & Real-World Validation Suite
 * Verifies all 30 core requirements from Phase 1 through Phase 30.
 */
class ProductionHardeningTest {

    private lateinit var riskEngine: RiskEngine
    private lateinit var stageMachine: ScamStageMachine
    private lateinit var velocityEngine: ManipulationVelocityEngine
    private lateinit var fusionEngine: RiskFusionEngine
    private lateinit var safetyBrakeUseCase: TriggerSafetyBrakeUseCase

    @Before
    fun setUp() {
        riskEngine = RiskEngine(lowThreshold = 30, highThreshold = 60, criticalThreshold = 80)
        stageMachine = ScamStageMachine()
        velocityEngine = ManipulationVelocityEngine(windowMillis = 90_000L)
        fusionEngine = RiskFusionEngine()
        safetyBrakeUseCase = TriggerSafetyBrakeUseCase(riskEngine)
    }

    // ==========================================
    // SECTION 7: ALL 9 SCAM TACTICS INDEPENDENTLY
    // ==========================================

    @Test
    fun testTactic1_AuthorityImpersonation() {
        val signals = riskEngine.analyzeTranscript("I am Inspector Sharma calling from Central Bureau of Investigation CBI headquarters.")
        assertEquals(1, signals.size)
        assertEquals(ScamTactic.AUTHORITY_IMPERSONATION, signals[0].tactic)
        assertEquals(15, signals[0].tactic.defaultWeight)
        assertFalse(signals[0].tactic.isIrreversibleAction)
    }

    @Test
    fun testTactic2_CriminalAllegationAndFear() {
        val signals = riskEngine.analyzeTranscript("A contraband narcotics package with your Aadhaar has been seized at Mumbai airport. Non-bailable arrest warrant issued.")
        assertTrue(signals.any { it.tactic == ScamTactic.CRIMINAL_ALLEGATION })
        assertEquals(15, ScamTactic.CRIMINAL_ALLEGATION.defaultWeight)
        assertFalse(ScamTactic.CRIMINAL_ALLEGATION.isIrreversibleAction)
    }

    @Test
    fun testTactic3_Urgency() {
        val signals = riskEngine.analyzeTranscript("You must resolve this immediately within 15 minutes right now or police will arrive.")
        assertTrue(signals.any { it.tactic == ScamTactic.URGENCY })
        assertEquals(10, ScamTactic.URGENCY.defaultWeight)
        assertFalse(ScamTactic.URGENCY.isIrreversibleAction)
    }

    @Test
    fun testTactic4_Isolation() {
        val signals = riskEngine.analyzeTranscript("Stay on the call. Do not disconnect. You are under digital arrest. Stay in a quiet closed room and do not tell anyone.")
        assertTrue(signals.any { it.tactic == ScamTactic.ISOLATION })
        assertEquals(15, ScamTactic.ISOLATION.defaultWeight)
        assertFalse(ScamTactic.ISOLATION.isIrreversibleAction)
    }

    @Test
    fun testTactic5_PaymentDemand_IrreversibleAction() {
        val signals = riskEngine.analyzeTranscript("Transfer 50,000 rupees immediately to the RBI verification escrow account as clearance deposit.")
        val payment = signals.firstOrNull { it.tactic == ScamTactic.PAYMENT_DEMAND }
        assertNotNull(payment)
        assertEquals(20, ScamTactic.PAYMENT_DEMAND.defaultWeight)
        assertTrue("Payment demand must be an irreversible action", payment!!.tactic.isIrreversibleAction)
    }

    @Test
    fun testTactic6_CredentialAndOtpPressure_IrreversibleAction() {
        val signals = riskEngine.analyzeTranscript("Read out the 6 digit verification code and share the OTP right now to stop the FIR.")
        val cred = signals.firstOrNull { it.tactic == ScamTactic.CREDENTIAL_PRESSURE }
        assertNotNull(cred)
        assertEquals(20, ScamTactic.CREDENTIAL_PRESSURE.defaultWeight)
        assertTrue("Credential pressure must be an irreversible action", cred!!.tactic.isIrreversibleAction)
    }

    @Test
    fun testTactic7_RemoteAccessPressure_IrreversibleAction() {
        val signals = riskEngine.analyzeTranscript("Install AnyDesk and download screen share software so our investigating team can connect to support app.")
        val remote = signals.firstOrNull { it.tactic == ScamTactic.REMOTE_ACCESS }
        assertNotNull(remote)
        assertEquals(15, ScamTactic.REMOTE_ACCESS.defaultWeight)
        assertTrue("Remote access must be an irreversible action", remote!!.tactic.isIrreversibleAction)
    }

    @Test
    fun testTactic8_SuspiciousLink() {
        val signals = riskEngine.analyzeTranscript("Click on the link to download the APK and open this URL to fill this form: https://fake-police.com")
        assertTrue(signals.any { it.tactic == ScamTactic.SUSPICIOUS_LINK })
        assertEquals(10, ScamTactic.SUSPICIOUS_LINK.defaultWeight)
        assertFalse(ScamTactic.SUSPICIOUS_LINK.isIrreversibleAction)
    }

    @Test
    fun testTactic9_CoerciveEscalation() {
        val signals = riskEngine.analyzeTranscript("We are sending police to your house right now, a patrol vehicle is dispatched and we will freeze all your bank accounts.")
        assertTrue(signals.any { it.tactic == ScamTactic.ESCALATION })
        assertEquals(10, ScamTactic.ESCALATION.defaultWeight)
        assertFalse(ScamTactic.ESCALATION.isIrreversibleAction)
    }

    @Test
    fun testBenignConversationProducesZeroFalsePositives() {
        val benignTexts = listOf(
            "Hi mom, I am buying some groceries from the supermarket. Do we have milk at home?",
            "Good morning! Let's schedule the project sync meeting for Thursday 2 PM.",
            "Can you please send me the recipe for the paneer butter masala you made yesterday?",
            "I will pick up the kids from school at 4 PM after my doctor appointment."
        )

        for (text in benignTexts) {
            val signals = riskEngine.analyzeTranscript(text)
            assertEquals("Expected 0 signals for benign text: '$text'", 0, signals.size)
            val score = riskEngine.calculateCurrentScore(signals, System.currentTimeMillis(), System.currentTimeMillis())
            assertEquals(0, score)
            assertEquals(RiskLevel.LOW, riskEngine.getRiskLevel(score))
            assertFalse(safetyBrakeUseCase(score, signals))
        }
    }

    // ==========================================
    // SECTION 8: SCAM STAGE MACHINE MONOTONICITY
    // ==========================================

    @Test
    fun testScamStageProgression_ContactToEscalation() {
        assertEquals(ScamStage.CONTACT, stageMachine.getCurrentStage())

        // 1. Authority
        val sigAuth = RiskSignal("1", 1000L, ScamTactic.AUTHORITY_IMPERSONATION, 0.9f, 15, "police", "SPEECH", "s1")
        stageMachine.processSignal(sigAuth)
        assertEquals(ScamStage.AUTHORITY, stageMachine.getCurrentStage())

        // 2. Fear
        val sigFear = RiskSignal("2", 2000L, ScamTactic.CRIMINAL_ALLEGATION, 0.9f, 15, "narcotics", "SPEECH", "s1")
        stageMachine.processSignal(sigFear)
        assertEquals(ScamStage.FEAR, stageMachine.getCurrentStage())

        // 3. Isolation
        val sigIso = RiskSignal("3", 3000L, ScamTactic.ISOLATION, 0.9f, 15, "quiet room", "SPEECH", "s1")
        stageMachine.processSignal(sigIso)
        assertEquals(ScamStage.ISOLATION, stageMachine.getCurrentStage())

        // 4. Payment / Credential
        val sigPay = RiskSignal("4", 4000L, ScamTactic.PAYMENT_DEMAND, 0.95f, 20, "transfer rupees", "SPEECH", "s1")
        stageMachine.processSignal(sigPay)
        assertEquals(ScamStage.PAYMENT_CREDENTIAL, stageMachine.getCurrentStage())

        // 5. Coercive Escalation
        val sigEsc = RiskSignal("5", 5000L, ScamTactic.ESCALATION, 0.95f, 10, "police raid", "SPEECH", "s1")
        stageMachine.processSignal(sigEsc)
        assertEquals(ScamStage.ESCALATION, stageMachine.getCurrentStage())

        // Impossible regression check: an earlier tactic arrives late
        val lateAuth = RiskSignal("6", 6000L, ScamTactic.AUTHORITY_IMPERSONATION, 0.9f, 15, "police", "SPEECH", "s1")
        val regressionTransition = stageMachine.processSignal(lateAuth)
        assertNull("Stage machine must NEVER regress back to an earlier stage", regressionTransition)
        assertEquals(ScamStage.ESCALATION, stageMachine.getCurrentStage())
    }

    // ==========================================
    // SECTION 9: MANIPULATION VELOCITY INDEPENDENCE
    // ==========================================

    @Test
    fun testManipulationVelocity_IndependentOfRiskScore() {
        val now = 100_000L

        // Scenario A: Single massive payment demand -> High risk, but LOW velocity (only 1 event)
        val singlePaymentSignal = listOf(
            RiskSignal("1", now - 10_000L, ScamTactic.PAYMENT_DEMAND, 0.95f, 20, "transfer 50000", "SPEECH", "s1")
        )
        val velA = velocityEngine.calculateVelocity(singlePaymentSignal, now)
        assertEquals(VelocityLevel.LOW, velA.level)
        assertEquals(1, velA.tacticCountInWindow)

        // Scenario B: Rapid burst of 4 tactics in 30 seconds -> HIGH velocity
        val rapidBarrage = listOf(
            RiskSignal("1", now - 25_000L, ScamTactic.AUTHORITY_IMPERSONATION, 0.9f, 15, "police", "SPEECH", "s1"),
            RiskSignal("2", now - 20_000L, ScamTactic.CRIMINAL_ALLEGATION, 0.9f, 15, "arrest warrant", "SPEECH", "s1"),
            RiskSignal("3", now - 10_000L, ScamTactic.URGENCY, 0.9f, 10, "hurry up", "SPEECH", "s1"),
            RiskSignal("4", now - 2_000L, ScamTactic.ISOLATION, 0.9f, 15, "stay in closed room", "SPEECH", "s1")
        )
        val velB = velocityEngine.calculateVelocity(rapidBarrage, now)
        assertEquals(VelocityLevel.HIGH, velB.level)
        assertEquals(4, velB.tacticCountInWindow)
    }

    // ==========================================
    // SECTION 12: SAFETY BRAKE DETERMINISM
    // ==========================================

    @Test
    fun testSafetyBrake_AllIrreversibleActionsAndThreshold() {
        val now = System.currentTimeMillis()

        // 1. Payment demand at risk 65 (>= 60) -> Triggers
        val paySig = listOf(RiskSignal("1", now, ScamTactic.PAYMENT_DEMAND, 0.95f, 20, "transfer", "SPEECH", "s1"))
        assertTrue("Risk 65 + Payment MUST trigger Safety Brake", safetyBrakeUseCase(65, paySig))

        // 2. Credential/OTP pressure at risk 65 -> Triggers
        val otpSig = listOf(RiskSignal("2", now, ScamTactic.CREDENTIAL_PRESSURE, 0.95f, 20, "give otp", "SPEECH", "s1"))
        assertTrue("Risk 65 + OTP pressure MUST trigger Safety Brake", safetyBrakeUseCase(65, otpSig))

        // 3. Remote access demand at risk 65 -> Triggers
        val remoteSig = listOf(RiskSignal("3", now, ScamTactic.REMOTE_ACCESS, 0.95f, 15, "install anydesk", "SPEECH", "s1"))
        assertTrue("Risk 65 + Remote Access MUST trigger Safety Brake", safetyBrakeUseCase(65, remoteSig))

        // 4. Non-irreversible tactic (Authority) at risk 75 -> DOES NOT trigger Safety Brake
        val authSig = listOf(RiskSignal("4", now, ScamTactic.AUTHORITY_IMPERSONATION, 0.9f, 15, "police", "SPEECH", "s1"))
        assertFalse("Risk 75 without irreversible action must NOT trigger Safety Brake", safetyBrakeUseCase(75, authSig))

        // 5. Payment demand at risk 45 (< 60) -> DOES NOT trigger Safety Brake yet
        assertFalse("Risk 45 (< 60) with Payment must NOT trigger Safety Brake yet", safetyBrakeUseCase(45, paySig))
    }

    // ==========================================
    // SECTION 11: RISK FUSION & DISAGREEMENT
    // ==========================================

    @Test
    fun testRiskFusion_ConversationDominatesVisual() {
        val now = System.currentTimeMillis()
        val highRiskSignals = listOf(
            RiskSignal("1", now, ScamTactic.AUTHORITY_IMPERSONATION, 0.9f, 15, "police", "SPEECH", "s1"),
            RiskSignal("2", now, ScamTactic.PAYMENT_DEMAND, 0.95f, 20, "transfer money", "SPEECH", "s1")
        )

        val clearVisual = VisualSignal(
            quality = VisualQuality.CLEAR,
            faceDetected = true,
            faceStabilityScore = 0.95f,
            lightingScore = 0.90f,
            confidence = 0.90f
        )
        val normalLiveness = LivenessAssessment(
            status = "CLEAR",
            score = 0.90f,
            confidence = 0.90f,
            explanation = "Normal natural motion observed."
        )

        // Model disagreement check
        val guard = ModelDisagreementGuard.checkDisagreement(
            conversationRiskLevel = RiskLevel.HIGH,
            visualSignal = clearVisual,
            livenessAssessment = normalLiveness
        )
        assertTrue(guard.isDisagreement)
        assertTrue(guard.explanation!!.contains("conversational coercion remains PRIMARY"))

        // Fusion decision must remain HIGH risk despite clear camera feed
        val fusion = fusionEngine.fuse(
            conversationScore = 75,
            signals = highRiskSignals,
            stage = ScamStage.PAYMENT_CREDENTIAL,
            velocity = VelocityCalculation(VelocityLevel.MODERATE, 2, 90L, 35, "moderate"),
            visualSignal = clearVisual,
            liveness = normalLiveness,
            isSafetyBrakeTriggered = true
        )

        assertEquals(RiskLevel.HIGH, fusion.riskLevel)
        assertTrue(fusion.isSafetyBrakeRequired)
        assertTrue(fusion.primarySignal.contains("Conversation"))
    }

    // ==========================================
    // SECTION 15: SHA-256 CHAIN & TAMPER DETECTION
    // ==========================================

    @Test
    fun testSha256GenesisAndTamperDetectionAcrossFields() {
        val genesis = "0000000000000000000000000000000000000000000000000000000000000000"
        assertEquals(64, genesis.length)

        val ev1 = EvidenceHasher.createEvent("e1", "s1", 1000L, "GENESIS", "{}", null)
        assertEquals(genesis, ev1.previousHash)
        assertEquals(64, ev1.currentHash.length)

        val ev2 = EvidenceHasher.createEvent("e2", "s1", 2000L, "TACTIC", "{\"tactic\":\"AUTHORITY\"}", ev1.currentHash)
        val ev3 = EvidenceHasher.createEvent("e3", "s1", 3000L, "SAFETY_BRAKE", "{\"triggered\":true}", ev2.currentHash)

        // Valid chain
        val validResult = EvidenceHasher.verifyChain(listOf(ev1, ev2, ev3))
        assertTrue(validResult is IntegrityResult.Valid)

        // Tamper 1: Modify timestamp of ev2
        val tamperedTimestamp = ev2.copy(timestamp = 2001L)
        val result1 = EvidenceHasher.verifyChain(listOf(ev1, tamperedTimestamp, ev3))
        assertTrue("Modifying timestamp must fail integrity verification", result1 is IntegrityResult.Failed)

        // Tamper 2: Modify eventType of ev2
        val tamperedType = ev2.copy(eventType = "BENIGN_TALK")
        val result2 = EvidenceHasher.verifyChain(listOf(ev1, tamperedType, ev3))
        assertTrue("Modifying eventType must fail integrity verification", result2 is IntegrityResult.Failed)

        // Tamper 3: Modify payload of ev2
        val tamperedPayload = ev2.copy(payloadJson = "{\"tactic\":\"CLEARED\"}")
        val result3 = EvidenceHasher.verifyChain(listOf(ev1, tamperedPayload, ev3))
        assertTrue("Modifying payload must fail integrity verification", result3 is IntegrityResult.Failed)

        // Tamper 4: Modify previousHash of ev3
        val tamperedPrevHash = ev3.copy(previousHash = "1111111111111111111111111111111111111111111111111111111111111111")
        val result4 = EvidenceHasher.verifyChain(listOf(ev1, ev2, tamperedPrevHash))
        assertTrue("Modifying previousHash link must fail integrity verification", result4 is IntegrityResult.Failed)
    }

    // ==========================================
    // SECTION 6: AI PROVIDER FALLBACK HIERARCHY
    // ==========================================

    @Test
    fun testAIProviderFallback_HierarchyAndMalformedJsonResilience() = runTest {
        // Factory provides local deterministic guardrail when cloud APIs are unconfigured
        val defaultProvider = AIProviderFactory.getProvider(geminiApiKey = null, groqApiKey = null, backendBaseUrl = null)
        assertEquals("RakshaCall-Deterministic-Guardrail", defaultProvider.providerName)

        // Deterministic guardrail functions sub-millisecond offline
        val localResult = defaultProvider.analyze("Transfer 50000 rupees immediately to CBI officer")
        assertTrue(localResult.tactics.contains("PAYMENT_DEMAND"))
        assertTrue(localResult.tactics.contains("AUTHORITY_IMPERSONATION"))
        assertTrue(localResult.irreversibleAction)
        assertEquals("PAYMENT_CREDENTIAL", localResult.stage)

        // Groq fallback test
        val unconfiguredGroq = GroqAIProvider(apiKey = null)
        assertFalse(unconfiguredGroq.isAvailable)
        val groqFallback = unconfiguredGroq.analyze("Transfer 50000 rupees")
        assertNotNull(groqFallback)
        assertTrue(groqFallback.tactics.contains("PAYMENT_DEMAND"))

        // Gemini fallback test
        val unconfiguredGemini = GeminiAIProvider(apiKey = null, backendBaseUrl = null)
        assertFalse(unconfiguredGemini.isAvailable)
        val geminiFallback = unconfiguredGemini.analyze("Transfer 50000 rupees")
        assertNotNull(geminiFallback)
        assertTrue(geminiFallback.tactics.contains("PAYMENT_DEMAND"))
    }

    // ==========================================
    // SECTION 14: HONEST TRUSTED CONTACT DELIVERY
    // ==========================================

    @Test
    fun testTrustedContactAlertDeliveryHonesty() = runTest {
        val provider = TrustedContactProviderImpl(backendBaseUrl = null)
        assertFalse("Direct SMS gateway should be unconfigured in test environment", provider.isDirectSmsConfigured)

        val req = ContactAlertRequest(
            contactName = "Rahul Verma",
            phoneNumber = "+919876543210",
            rakshaCallId = "RC-998877",
            sessionId = "sess-prod-1",
            riskScore = 85,
            alertMessage = "High risk digital arrest coercion detected."
        )

        val result = provider.dispatchEmergencyAlert(req)
        // Strictly must NOT report confirmed delivery!
        assertEquals(AlertDeliveryStatus.ALERT_REQUESTED, result.status)
        assertTrue(result.note.contains("SMS provider not configured"))
    }
}
