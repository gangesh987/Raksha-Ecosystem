package com.rakshacall.safety

import com.rakshacall.safety.core.security.CryptographyHelper
import com.rakshacall.safety.core.security.EvidenceHasher
import com.rakshacall.safety.core.security.IntegrityResult
import com.rakshacall.safety.core.security.KeystoreManager
import com.rakshacall.safety.core.speech.LanguageAwareTacticEngine
import com.rakshacall.safety.domain.engine.ManipulationVelocityEngine
import com.rakshacall.safety.domain.engine.RiskEngine
import com.rakshacall.safety.domain.engine.RiskFusionEngine
import com.rakshacall.safety.domain.engine.ScamStageMachine
import com.rakshacall.safety.domain.model.ConsentType
import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.SessionStatus
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.domain.model.TrustedContact
import com.rakshacall.safety.domain.model.User
import com.rakshacall.safety.domain.repository.EvidenceRepository
import com.rakshacall.safety.domain.repository.RiskRepository
import com.rakshacall.safety.domain.repository.SessionRepository
import com.rakshacall.safety.domain.usecase.AnalyzeRiskUseCase
import com.rakshacall.safety.domain.usecase.FuseRiskSignalsUseCase
import com.rakshacall.safety.domain.usecase.ProcessTranscriptUseCase
import com.rakshacall.safety.domain.usecase.StartProtectionSessionUseCase
import com.rakshacall.safety.domain.usecase.TriggerSafetyBrakeUseCase
import com.rakshacall.safety.domain.usecase.UpdateScamStageUseCase
import com.rakshacall.safety.domain.usecase.VerifyEvidenceIntegrityUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class Phase2ProductionArchitectureTest {

    private lateinit var riskEngine: RiskEngine
    private lateinit var stageMachine: ScamStageMachine
    private lateinit var velocityEngine: ManipulationVelocityEngine
    private lateinit var riskFusionEngine: RiskFusionEngine
    private lateinit var languageTacticEngine: LanguageAwareTacticEngine

    // In-memory test repositories
    private val inMemorySessions = mutableListOf<ProtectionSession>()
    private val inMemoryEvidence = mutableListOf<EvidenceEvent>()
    private val inMemoryTranscripts = mutableListOf<TranscriptEvent>()
    private val inMemorySignals = mutableListOf<RiskSignal>()
    private val inMemoryPoints = mutableListOf<Pair<Long, Int>>()

    private val sessionRepo = object : SessionRepository {
        override suspend fun createSession(session: ProtectionSession) { inMemorySessions.add(session) }
        override suspend fun getSession(sessionId: String): ProtectionSession? = inMemorySessions.firstOrNull { it.id == sessionId }
        override fun observeActiveSession(): Flow<ProtectionSession?> = flowOf(inMemorySessions.firstOrNull())
        override fun observeAllSessions(): Flow<List<ProtectionSession>> = flowOf(inMemorySessions)
        override suspend fun updateSession(session: ProtectionSession) {}
        override suspend fun endSession(sessionId: String, finalRisk: Int, peakRisk: Int) {}
        override suspend fun deleteSession(sessionId: String) {}
        override suspend fun clearAllSessions() { inMemorySessions.clear() }
    }

    private val evidenceRepo = object : EvidenceRepository {
        override suspend fun appendEvidence(event: EvidenceEvent) { inMemoryEvidence.add(event) }
        override fun observeEvidence(sessionId: String): Flow<List<EvidenceEvent>> = flowOf(inMemoryEvidence.filter { it.sessionId == sessionId })
        override suspend fun getEvidenceForSession(sessionId: String): List<EvidenceEvent> = inMemoryEvidence.filter { it.sessionId == sessionId }
        override suspend fun getLastEvidenceHash(sessionId: String): String? = inMemoryEvidence.lastOrNull { it.sessionId == sessionId }?.currentHash
        override suspend fun clearAllEvidence() { inMemoryEvidence.clear() }
    }

    private val riskRepo = object : RiskRepository {
        override suspend fun insertTranscriptEvent(event: TranscriptEvent) { inMemoryTranscripts.add(event) }
        override fun observeTranscripts(sessionId: String): Flow<List<TranscriptEvent>> = flowOf(inMemoryTranscripts)
        override suspend fun getTranscripts(sessionId: String): List<TranscriptEvent> = inMemoryTranscripts
        override suspend fun insertRiskSignal(signal: RiskSignal) { inMemorySignals.add(signal) }
        override fun observeRiskSignals(sessionId: String): Flow<List<RiskSignal>> = flowOf(inMemorySignals)
        override suspend fun getRiskSignals(sessionId: String): List<RiskSignal> = inMemorySignals
        override suspend fun recordRiskPoint(sessionId: String, timestamp: Long, riskScore: Int) { inMemoryPoints.add(Pair(timestamp, riskScore)) }
        override fun observeRiskPoints(sessionId: String): Flow<List<Pair<Long, Int>>> = flowOf(inMemoryPoints)
        override suspend fun getRiskPoints(sessionId: String): List<Pair<Long, Int>> = inMemoryPoints
    }

    @Before
    fun setUp() {
        riskEngine = RiskEngine()
        stageMachine = ScamStageMachine()
        velocityEngine = ManipulationVelocityEngine()
        riskFusionEngine = RiskFusionEngine()
        languageTacticEngine = LanguageAwareTacticEngine()

        inMemorySessions.clear()
        inMemoryEvidence.clear()
        inMemoryTranscripts.clear()
        inMemorySignals.clear()
        inMemoryPoints.clear()
    }

    @Test
    fun testStartProtectionSessionUseCaseCreatesSessionAndGenesisEvidence() = runTest {
        val useCase = StartProtectionSessionUseCase(sessionRepo, evidenceRepo)
        val session = useCase(sessionId = "test-session-123", isDemoSession = false)

        assertEquals("test-session-123", session.id)
        assertEquals(SessionStatus.ACTIVE, session.status)
        assertEquals(ScamStage.CONTACT, session.highestStage)
        assertFalse(session.isDemoSession)

        // Verifies genesis evidence was appended
        assertEquals(1, inMemoryEvidence.size)
        assertEquals("GENESIS", inMemoryEvidence[0].eventType)
        assertEquals(EvidenceHasher.GENESIS_PREVIOUS_HASH, inMemoryEvidence[0].previousHash)
    }

    @Test
    fun testProcessTranscriptUseCaseDetectsTacticsAndPersists() = runTest {
        val useCase = ProcessTranscriptUseCase(riskRepo, riskEngine)
        val (event, signals) = useCase(
            sessionId = "session-1",
            speaker = "CALLER",
            text = "This is Mumbai Police Cyber Crime Cell. Transfer 50000 rupees immediately to verify.",
            pastSignals = emptyList()
        )

        assertNotNull(event)
        assertTrue(signals.size >= 2)
        assertEquals(1, inMemoryTranscripts.size)
        assertTrue(inMemorySignals.size >= 2)
    }

    @Test
    fun testAnalyzeRiskUseCaseProducesExplainableDecision() = runTest {
        val useCase = AnalyzeRiskUseCase(riskRepo, riskEngine)
        val signals = listOf(
            RiskSignal("s1", System.currentTimeMillis(), ScamTactic.AUTHORITY_IMPERSONATION, 0.9f, 15, "police", "SPEECH", "sess-1"),
            RiskSignal("s2", System.currentTimeMillis(), ScamTactic.CRIMINAL_ALLEGATION, 0.9f, 15, "drugs found", "SPEECH", "sess-1"),
            RiskSignal("s3", System.currentTimeMillis(), ScamTactic.PAYMENT_DEMAND, 0.95f, 20, "transfer rupees", "SPEECH", "sess-1")
        )

        val decision = useCase("sess-1", signals, System.currentTimeMillis())
        assertTrue(decision.score >= 50)
        assertNotNull(decision.recommendedAction)
        assertTrue(decision.reasons.any { it.contains("Irreversible") })
        assertEquals(1, inMemoryPoints.size)
    }

    @Test
    fun testScamStageProgressionUseCaseMonotonicTransitions() {
        val useCase = UpdateScamStageUseCase(stageMachine)
        val sig1 = RiskSignal("1", System.currentTimeMillis(), ScamTactic.AUTHORITY_IMPERSONATION, 0.9f, 15, "police", "SPEECH", "s1")
        val stage1 = useCase(sig1)
        assertEquals(ScamStage.AUTHORITY, stage1)

        val sig2 = RiskSignal("2", System.currentTimeMillis(), ScamTactic.PAYMENT_DEMAND, 0.95f, 20, "transfer", "SPEECH", "s1")
        val stage2 = useCase(sig2)
        assertEquals(ScamStage.PAYMENT_CREDENTIAL, stage2)

        // Lower tactic does not regress stage
        val sig3 = RiskSignal("3", System.currentTimeMillis(), ScamTactic.URGENCY, 0.9f, 10, "hurry up", "SPEECH", "s1")
        val stage3 = useCase(sig3)
        assertEquals(ScamStage.PAYMENT_CREDENTIAL, stage3)
    }

    @Test
    fun testSafetyBrakeUseCaseTrigger() {
        val useCase = TriggerSafetyBrakeUseCase(riskEngine)
        val nonIrreversibleSignals = listOf(
            RiskSignal("1", System.currentTimeMillis(), ScamTactic.AUTHORITY_IMPERSONATION, 0.9f, 15, "police", "SPEECH", "s1"),
            RiskSignal("2", System.currentTimeMillis(), ScamTactic.CRIMINAL_ALLEGATION, 0.9f, 15, "arrest warrant", "SPEECH", "s1")
        )
        // High score without irreversible action does NOT trigger Safety Brake
        assertFalse(useCase(75, nonIrreversibleSignals))

        // High score WITH payment demand DOES trigger Safety Brake
        val withPayment = nonIrreversibleSignals + RiskSignal("3", System.currentTimeMillis(), ScamTactic.PAYMENT_DEMAND, 0.95f, 20, "transfer money", "SPEECH", "s1")
        assertTrue(useCase(75, withPayment))
    }

    @Test
    fun testVerifyEvidenceIntegrityUseCaseDetectsTampering() = runTest {
        val useCase = VerifyEvidenceIntegrityUseCase(evidenceRepo)

        // Append 2 chained events
        val ev1 = EvidenceHasher.createEvent("1", "s1", 1000L, "GENESIS", "{}", null)
        evidenceRepo.appendEvidence(ev1)
        val ev2 = EvidenceHasher.createEvent("2", "s1", 1010L, "TACTIC", "{\"tactic\":\"AUTHORITY\"}", ev1.currentHash)
        evidenceRepo.appendEvidence(ev2)

        val validResult = useCase("s1")
        assertTrue(validResult is IntegrityResult.Valid)

        // Tamper with payload of ev2 in memory
        inMemoryEvidence[1] = ev2.copy(payloadJson = "{\"tactic\":\"ALTERED_PAYLOAD\"}")
        val tamperedResult = useCase("s1")
        assertTrue(tamperedResult is IntegrityResult.Failed)
    }

    @Test
    fun testLanguageAwareTacticEngineDetectsHindiCoercion() {
        val event = TranscriptEvent(
            id = "hi-1",
            sessionId = "sess-hi",
            timestamp = System.currentTimeMillis(),
            speaker = "CALLER",
            text = "हम मुंबई पुलिस क्राइम ब्रांच से बात कर रहे हैं। आपके आधार पर अरेस्ट वारंट है। पचास हजार तुरंत ट्रांसफर करो।"
        )

        val signals = languageTacticEngine.analyze(event, "hi-IN")
        assertTrue("Expected authority tactic in Hindi", signals.any { it.tactic == ScamTactic.AUTHORITY_IMPERSONATION })
        assertTrue("Expected criminal allegation in Hindi", signals.any { it.tactic == ScamTactic.CRIMINAL_ALLEGATION })
        assertTrue("Expected urgency in Hindi", signals.any { it.tactic == ScamTactic.URGENCY })
        assertTrue("Expected payment demand in Hindi", signals.any { it.tactic == ScamTactic.PAYMENT_DEMAND })
    }

    @Test
    fun testKeystoreEncryptionAndDecryptionRoundtrip() {
        val originalSecret = "{\"evidence\":\"Aadhaar compromised\",\"amount\":50000}"
        val encrypted = KeystoreManager.encrypt(originalSecret)
        assertNotNull(encrypted)
        assertFalse(encrypted == originalSecret)

        val decrypted = KeystoreManager.decrypt(encrypted)
        assertEquals(originalSecret, decrypted)
    }
}
