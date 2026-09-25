package com.rakshacall.safety

import com.rakshacall.safety.data.provider.AIProviderFactory
import com.rakshacall.safety.data.provider.AuthenticationProviderImpl
import com.rakshacall.safety.data.provider.CloudSyncProviderImpl
import com.rakshacall.safety.data.provider.GeminiAIProvider
import com.rakshacall.safety.data.provider.GroqAIProvider
import com.rakshacall.safety.data.provider.LocalSafetyProvider
import com.rakshacall.safety.data.provider.TrustedContactProviderImpl
import com.rakshacall.safety.data.sync.SyncManager
import com.rakshacall.safety.domain.engine.LivenessAssessment
import com.rakshacall.safety.domain.engine.ModelDisagreementGuard
import com.rakshacall.safety.domain.engine.RiskEngine
import com.rakshacall.safety.domain.engine.RiskFusionEngine
import com.rakshacall.safety.domain.engine.ScamStageMachine
import com.rakshacall.safety.domain.model.AlertDeliveryStatus
import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.User
import com.rakshacall.safety.domain.model.VisualQuality
import com.rakshacall.safety.domain.model.VisualSignal
import com.rakshacall.safety.domain.provider.AIConnectionState
import com.rakshacall.safety.domain.provider.AuthResult
import com.rakshacall.safety.domain.provider.ContactAlertRequest
import com.rakshacall.safety.domain.repository.EvidenceRepository
import com.rakshacall.safety.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase3FinalArchitectureTest {

    private lateinit var localSafetyProvider: LocalSafetyProvider
    private lateinit var riskEngine: RiskEngine
    private lateinit var stageMachine: ScamStageMachine
    private lateinit var fusionEngine: RiskFusionEngine

    private var currentUser: User? = null

    private val mockUserRepo = object : UserRepository {
        override suspend fun saveUser(user: User) { currentUser = user }
        override suspend fun getActiveUser(): User? = currentUser
        override fun observeActiveUser(): Flow<User?> = flowOf(currentUser)
        override suspend fun clearUser() { currentUser = null }
    }

    private val inMemoryEvidence = mutableListOf<EvidenceEvent>()
    private val mockEvidenceRepo = object : EvidenceRepository {
        override suspend fun appendEvidence(event: EvidenceEvent) { inMemoryEvidence.add(event) }
        override fun observeEvidence(sessionId: String): Flow<List<EvidenceEvent>> = flowOf(inMemoryEvidence)
        override suspend fun getEvidenceForSession(sessionId: String): List<EvidenceEvent> = inMemoryEvidence
        override suspend fun getLastEvidenceHash(sessionId: String): String? = inMemoryEvidence.lastOrNull()?.currentHash
        override suspend fun clearAllEvidence() { inMemoryEvidence.clear() }
    }

    @Before
    fun setUp() {
        riskEngine = RiskEngine()
        stageMachine = ScamStageMachine()
        fusionEngine = RiskFusionEngine()
        localSafetyProvider = LocalSafetyProvider(riskEngine)
        currentUser = null
        inMemoryEvidence.clear()
    }

    @Test
    fun testLocalSafetyProviderDetectsCoerciveTacticsAndIrreversibleAction() = runTest {
        val result = localSafetyProvider.analyze(
            "This is Mumbai Police Cyber Crime. Your Aadhaar is implicated in money laundering. Transfer 50000 rupees to RBI verification account."
        )

        assertTrue(result.tactics.contains("AUTHORITY_IMPERSONATION"))
        assertTrue(result.tactics.contains("CRIMINAL_ALLEGATION"))
        assertTrue(result.tactics.contains("PAYMENT_DEMAND"))
        assertTrue("Expected irreversibleAction to be true", result.irreversibleAction)
        assertEquals("PAYMENT_CREDENTIAL", result.stage)
        assertTrue(result.riskContribution >= 50)
        assertTrue(result.recommendedAction.contains("DO NOT transfer money"))
    }

    @Test
    fun testGeminiAIProviderGracefulFallbackWhenUnconfigured() = runTest {
        val unconfiguredGemini = GeminiAIProvider(apiKey = null, backendBaseUrl = null)
        assertFalse(unconfiguredGemini.isAvailable)
        assertEquals(AIConnectionState.AI_UNAVAILABLE, unconfiguredGemini.connectionState)

        val result = unconfiguredGemini.analyze("Transfer money immediately to police")
        assertNotNull(result)
        assertTrue(result.tactics.contains("PAYMENT_DEMAND") || result.tactics.contains("AUTHORITY_IMPERSONATION"))
        assertTrue(result.modelVersion.contains("local-deterministic-guardrail"))
    }

    @Test
    fun testGroqAIProviderGracefulFallbackWhenUnconfigured() = runTest {
        val unconfiguredGroq = GroqAIProvider(apiKey = null)
        assertFalse(unconfiguredGroq.isAvailable)
        assertEquals(AIConnectionState.AI_UNAVAILABLE, unconfiguredGroq.connectionState)

        val result = unconfiguredGroq.analyze("Transfer money immediately to police")
        assertNotNull(result)
        assertTrue(result.tactics.contains("PAYMENT_DEMAND") || result.tactics.contains("AUTHORITY_IMPERSONATION"))
        assertTrue(result.modelVersion.contains("local-deterministic-guardrail"))
    }

    @Test
    fun testAIProviderFactoryProvidesGuardrail() {
        val provider = AIProviderFactory.getProvider(geminiApiKey = null, groqApiKey = null, backendBaseUrl = null)
        assertNotNull(provider)
        assertEquals("RakshaCall-Deterministic-Guardrail", provider.providerName)
    }

    @Test
    fun testAuthenticationProviderGeneratesUniqueRakshaCallIdAndRejectsInvalidPhone() = runTest {
        val authProvider = AuthenticationProviderImpl(mockUserRepo, firebaseEnabled = false)

        val invalidResult = authProvider.requestPhoneOtp("123")
        assertTrue(invalidResult is AuthResult.Error)

        val validResult = authProvider.requestPhoneOtp("9876543210")
        assertTrue(validResult is AuthResult.Success)
        val user = (validResult as AuthResult.Success).user
        assertTrue(user.rakshaCallId.startsWith("RC-"))
        assertEquals(9, user.rakshaCallId.length) // RC- + 6 digits
        assertEquals("9876543210", user.phoneNumber)
    }

    @Test
    fun testAuthenticationProviderEmailSignInAndSignUp() = runTest {
        val authProvider = AuthenticationProviderImpl(mockUserRepo, firebaseEnabled = false)

        val invalidEmailResult = authProvider.signUpWithEmail("invalid-email", "password123")
        assertTrue(invalidEmailResult is AuthResult.Error)

        val shortPasswordResult = authProvider.signUpWithEmail("user@example.com", "123")
        assertTrue(shortPasswordResult is AuthResult.Error)

        val validSignUp = authProvider.signUpWithEmail("victim.safe@gmail.com", "SecurePassword123")
        assertTrue(validSignUp is AuthResult.Success)
        val user = (validSignUp as AuthResult.Success).user
        assertTrue(user.rakshaCallId.startsWith("RC-"))
        assertEquals("victim.safe@gmail.com", user.email)
    }

    @Test
    fun testEvidenceEventSha256CalculationMatchesImmutabilityStandard() {
        val genesisHash = "0000000000000000000000000000000000000000000000000000000000000000"
        val hash = EvidenceEvent.calculateHash(
            previousHash = genesisHash,
            eventId = "evt_001",
            timestamp = 1726228800000L,
            eventType = "TACTIC_DETECTED",
            payload = "AUTHORITY_IMPERSONATION"
        )
        assertNotNull(hash)
        assertEquals(64, hash.length) // SHA-256 hex string length

        // Tampering payload must alter the hash
        val tamperedHash = EvidenceEvent.calculateHash(
            previousHash = genesisHash,
            eventId = "evt_001",
            timestamp = 1726228800000L,
            eventType = "TACTIC_DETECTED",
            payload = "PAYMENT_DEMAND"
        )
        assertFalse(hash == tamperedHash)
    }

    @Test
    fun testTrustedContactProviderHonestDeliveryStatusWhenTwilioNotConfigured() = runTest {
        val provider = TrustedContactProviderImpl(backendBaseUrl = null)
        assertFalse(provider.isDirectSmsConfigured)

        val request = ContactAlertRequest(
            contactName = "Aarav Sharma",
            phoneNumber = "+919876543210",
            rakshaCallId = "RC-123456",
            sessionId = "sess-01",
            riskScore = 80,
            alertMessage = "High risk digital arrest detected"
        )

        val result = provider.dispatchEmergencyAlert(request)
        // Must NOT report confirmed delivery when Twilio is absent!
        assertEquals(AlertDeliveryStatus.ALERT_REQUESTED, result.status)
        assertTrue(result.note.contains("SMS provider not configured"))
    }

    @Test
    fun testCloudSyncProviderMaintainsLocalOnlyWhenCloudDisabled() = runTest {
        val syncManager = SyncManager(mockEvidenceRepo)
        val provider = CloudSyncProviderImpl(syncManager, cloudEnabled = false)
        assertFalse(provider.isCloudEnabled)

        val result = provider.syncPendingSessions()
        assertTrue(result.isSuccess)
    }

    @Test
    fun testModelDisagreementWhenConversationIsHighRiskAndVisualIsNormal() {
        val visual = VisualSignal(
            quality = VisualQuality.CLEAR,
            faceDetected = true,
            faceStabilityScore = 0.90f,
            lightingScore = 0.85f,
            confidence = 0.90f
        )
        val liveness = LivenessAssessment(
            status = "CLEAR",
            score = 0.90f,
            confidence = 0.90f,
            explanation = "Normal facial micro-movements verified."
        )

        val disagreement = ModelDisagreementGuard.checkDisagreement(
            conversationRiskLevel = RiskLevel.HIGH,
            visualSignal = visual,
            livenessAssessment = liveness
        )

        assertTrue(disagreement.isDisagreement)
        assertTrue(disagreement.explanation?.contains("conversational coercion remains PRIMARY") == true)
    }

    @Test
    fun testRealisticEndToEndProtectionFlow() {
        // Step 1: Normal conversation
        val normalSignals = riskEngine.analyzeTranscript("Hello, who is this?")
        assertEquals(0, normalSignals.size)

        // Step 2: Authority
        val authSignals = riskEngine.analyzeTranscript("I am calling from Mumbai Police Cyber Crime Cell.")
        val authSignal = authSignals.first { it.tactic == ScamTactic.AUTHORITY_IMPERSONATION }
        stageMachine.processSignal(authSignal)
        assertEquals(ScamStage.AUTHORITY, stageMachine.getCurrentStage())

        // Step 3: Fear
        val fearSignals = riskEngine.analyzeTranscript("Your Aadhaar is involved in illegal money laundering.")
        val fearSignal = fearSignals.first { it.tactic == ScamTactic.CRIMINAL_ALLEGATION }
        stageMachine.processSignal(fearSignal)
        assertEquals(ScamStage.FEAR, stageMachine.getCurrentStage())

        // Step 4: Isolation
        val isoSignals = riskEngine.analyzeTranscript("Do not disconnect. Stay in a quiet closed room and don't tell your family.")
        val isoSignal = isoSignals.first { it.tactic == ScamTactic.ISOLATION }
        stageMachine.processSignal(isoSignal)
        assertEquals(ScamStage.ISOLATION, stageMachine.getCurrentStage())

        // Step 5: Payment demand (Irreversible action)
        val paymentSignals = riskEngine.analyzeTranscript("Transfer 50000 rupees immediately to the RBI verification account.")
        val paymentSignal = paymentSignals.first { it.tactic == ScamTactic.PAYMENT_DEMAND }
        stageMachine.processSignal(paymentSignal)
        assertEquals(ScamStage.PAYMENT_CREDENTIAL, stageMachine.getCurrentStage())

        // Step 6: Verify Safety Brake triggers on cumulative score >= 60 with payment demand
        val allSignals = authSignals + fearSignals + isoSignals + paymentSignals
        val now = System.currentTimeMillis()
        val finalScore = riskEngine.calculateCurrentScore(allSignals, now, now)
        assertTrue("Expected final score >= 60, got $finalScore", finalScore >= 60)
        val safetyBrakeFired = riskEngine.isSafetyBrakeTriggered(finalScore, allSignals)
        assertTrue("Safety Brake must trigger on high risk + payment demand", safetyBrakeFired)
    }
}
