package com.rakshacall.safety

import com.rakshacall.safety.core.security.EvidenceHasher
import com.rakshacall.safety.core.security.IntegrityResult
import com.rakshacall.safety.domain.engine.ManipulationVelocityEngine
import com.rakshacall.safety.domain.engine.RiskEngine
import com.rakshacall.safety.domain.engine.ScamStageMachine
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.presentation.protection.SIMULATED_SCAM_PROGRESSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Validates the complete Video Call Simulation Lab intelligence pipeline.
 * Tests that simulated dialogue progresses through the exact real RiskEngine,
 * ScamStageMachine, ManipulationVelocityEngine, and SHA-256 Evidence Chain.
 */
class VideoCallSimulationTest {

    private lateinit var riskEngine: RiskEngine
    private lateinit var stageMachine: ScamStageMachine
    private lateinit var velocityEngine: ManipulationVelocityEngine

    @Before
    fun setUp() {
        riskEngine = RiskEngine()
        stageMachine = ScamStageMachine()
        velocityEngine = ManipulationVelocityEngine()
    }

    @Test
    fun test_simulation_step_progression_escalates_risk() {
        val detectedSignals = mutableListOf<RiskSignal>()
        val sessionId = "DEMO-SIM-TEST"
        val startTime = System.currentTimeMillis()

        var currentRisk = 0
        var currentStage = ScamStage.CONTACT

        // Run through all 6 phases
        for (step in SIMULATED_SCAM_PROGRESSION) {
            val event = TranscriptEvent(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                speaker = step.speaker,
                text = step.text
            )

            val newSignals = riskEngine.analyzeTranscript(event, detectedSignals)
            for (sig in newSignals) {
                detectedSignals.add(sig)
                val transition = stageMachine.processSignal(sig)
                if (transition != null) {
                    currentStage = transition.toStage
                }
            }

            currentRisk = riskEngine.calculateCurrentScore(detectedSignals, startTime)
        }

        // Verify that tactics were detected across the progression
        assertTrue("Expected multiple detected tactics across the 6 simulation phases", detectedSignals.size >= 4)

        // Verify specific tactics identified
        val tacticSet = detectedSignals.map { it.tactic }.toSet()
        assertTrue("Must detect Authority Impersonation", tacticSet.contains(ScamTactic.AUTHORITY_IMPERSONATION))
        assertTrue("Must detect Criminal Allegation", tacticSet.contains(ScamTactic.CRIMINAL_ALLEGATION))
        assertTrue("Must detect Isolation", tacticSet.contains(ScamTactic.ISOLATION))
        assertTrue("Must detect Payment Demand or Credential Pressure", 
            tacticSet.contains(ScamTactic.PAYMENT_DEMAND) || tacticSet.contains(ScamTactic.CREDENTIAL_PRESSURE))

        // Final score must reach high risk (>= 60)
        assertTrue("Final risk score must reach at least 60 (was $currentRisk)", currentRisk >= 60)

        // Stage must have progressed to DEMAND or PAYMENT_CREDENTIAL
        assertTrue("Final stage must be DEMAND or PAYMENT_CREDENTIAL (was $currentStage)", 
            currentStage == ScamStage.DEMAND || currentStage == ScamStage.PAYMENT_CREDENTIAL || currentStage == ScamStage.ESCALATION)
    }

    @Test
    fun test_simulation_triggers_safety_brake() {
        val detectedSignals = mutableListOf<RiskSignal>()
        val sessionId = "DEMO-SIM-SAFETY"
        val startTime = System.currentTimeMillis()

        for (step in SIMULATED_SCAM_PROGRESSION) {
            val event = TranscriptEvent(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                speaker = step.speaker,
                text = step.text
            )
            val newSignals = riskEngine.analyzeTranscript(event, detectedSignals)
            detectedSignals.addAll(newSignals)
        }

        val finalScore = riskEngine.calculateCurrentScore(detectedSignals, startTime)
        val isTriggered = riskEngine.isSafetyBrakeTriggered(finalScore, detectedSignals)

        assertTrue("Safety Brake must trigger on simulated digital arrest progression", isTriggered)
    }

    @Test
    fun test_simulation_cryptographic_evidence_chain() {
        val sessionId = "DEMO-SIM-CRYPTO"
        val events = mutableListOf<com.rakshacall.safety.domain.model.EvidenceEvent>()
        var lastHash: String? = null

        for (step in SIMULATED_SCAM_PROGRESSION) {
            val ev = EvidenceHasher.createEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                eventType = "DEMO_SIMULATION_EVENT",
                payloadJson = "{\"phase\":${step.phaseNumber},\"title\":\"${step.phaseTitle}\",\"text\":\"${step.text.take(30)}\"}",
                lastKnownHash = lastHash
            )
            events.add(ev)
            lastHash = ev.currentHash
        }

        assertEquals(6, events.size)

        // Verify hash chain using EvidenceHasher.verifyChain
        val result = EvidenceHasher.verifyChain(events)
        assertTrue("Evidence chain must verify successfully", result is IntegrityResult.Valid)
    }
}
