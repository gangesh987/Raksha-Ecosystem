package com.rakshacall.safety

import com.rakshacall.safety.domain.engine.ManipulationVelocityEngine
import com.rakshacall.safety.domain.engine.ScamStageMachine
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.evidence.EvidenceSource
import com.rakshacall.safety.evidence.EvidenceVault
import com.rakshacall.safety.intelligence.ConversationMemory
import com.rakshacall.safety.intelligence.MultilingualSemanticEngine
import com.rakshacall.safety.intelligence.RakshaAIOrchestrator
import com.rakshacall.safety.intelligence.visual.VideoFrameSample
import com.rakshacall.safety.intelligence.visual.VisualThreatDetector
import com.rakshacall.safety.intelligence.visual.VisualThreatType
import com.rakshacall.safety.webrtc.WebRtcConfig
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.intelligence.ProtectionDecision
import com.rakshacall.safety.protection.EvidenceStrength
import com.rakshacall.safety.protection.ProtectionController
import com.rakshacall.safety.protection.ProtectionMode
import com.rakshacall.safety.protection.ProtectionPolicy
import com.rakshacall.safety.protection.ProtectionState
import com.rakshacall.safety.protection.UIMode
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RealtimeMultilingualAIPlatformTest {

    private val semanticEngine = MultilingualSemanticEngine()
    private val evidenceVault = EvidenceVault()
    private val visualDetector = VisualThreatDetector()

    @Test
    fun testWebRtcConfiguration() {
        val config = WebRtcConfig()
        assertTrue(config.isVideoEnabled)
        assertTrue(config.isAudioEnabled)
        assertTrue(config.iceServers.isNotEmpty())
        assertTrue(config.iceServers.any { it.urls.any { u -> u.contains("stun.l.google.com") } })
    }

    @Test
    fun testMultilingualDetectionAcross8Languages() {
        val testCases = listOf(
            "en" to "I am calling from the police department. Your account is linked to illegal money laundering.",
            "ta" to "நான் காவல் அதிகாரி பேசுகிறேன். உங்கள் மீது கைது வாரண்ட் பிறப்பிக்கப்பட்டுள்ளது.",
            "hi" to "मैं सीबीआई अधिकारी बोल रहा हूँ। आपका खाता गैरकानूनी लेनदेन में शामिल है।",
            "te" to "నేను పోలీస్ అధికారిని మాట్లాడుతున్నాను. మీపై అరెస్ట్ వారెంట్ జారీ చేయబడింది.",
            "kn" to "ನಾನು ಪೊಲೀಸ್ ಅಧಿಕಾರಿ ಮಾತನಾಡುತ್ತಿದ್ದೇನೆ. ನಿಮ್ಮ ಖಾತೆ ಕಾನೂನುಬಾಹಿರ ವಹಿವಾಟಿನಲ್ಲಿ ತೊಡಗಿದೆ.",
            "ml" to "ഞാൻ പോലീസ് ഉദ്യോഗസ്ഥനാണ് സംസാരിക്കുന്നത്. നിങ്ങൾക്ക് എതിരെ അറസ്റ്റ് വാറണ്ട് ഉണ്ട്.",
            "bn" to "আমি পুলিশ কর্মকর্তা বলছি। আপনার বিরুদ্ধে গ্রেপ্তারি পরোয়ানা জারি করা হয়েছে।",
            "mr" to "मी पोलीस अधिकारी बोलत आहे. तुमचे खाते बेकायदेशीर व्यवहारात अडकले आहे."
        )

        for ((expectedLang, phrase) in testCases) {
            val event = TranscriptEvent(
                id = UUID.randomUUID().toString(),
                sessionId = "sess-$expectedLang",
                text = phrase
            )
            val (langAnalysis, signals) = semanticEngine.analyze(event)

            assertEquals(expectedLang, langAnalysis.primaryLanguage)
            assertTrue("Expected signals for $expectedLang", signals.isNotEmpty())
            assertTrue(
                signals.any { it.tactic == ScamTactic.AUTHORITY_IMPERSONATION || it.tactic == ScamTactic.CRIMINAL_ALLEGATION }
            )
        }
    }

    @Test
    fun testRomanizedIndianLanguagesAndSlangNeutrality() {
        val romanizedCases = listOf(
            "unga account block aagidum machan panam anupunga", // Tamil
            "aapka account band ho jayega bhai paise transfer karo", // Hindi
            "mee account block aipothundi anna dabbulu pampandi", // Telugu
            "nimma account block agutte boss hana vargayisi", // Kannada
            "ningalude account block aakum chetta panam ayakku" // Malayalam
        )

        for (phrase in romanizedCases) {
            val event = TranscriptEvent(
                id = UUID.randomUUID().toString(),
                sessionId = "sess-romanized",
                text = phrase
            )
            val (lang, signals) = semanticEngine.analyze(event)
            assertTrue("Expected tactics in romanized phrase: $phrase", signals.isNotEmpty())
            assertTrue(signals.any { it.tactic == ScamTactic.PAYMENT_DEMAND || it.tactic == ScamTactic.CRIMINAL_ALLEGATION })
        }
    }

    @Test
    fun testPhoneticAsrErrorsAndIndirectIntent() {
        // "read those six numbers" + "OTB"
        val event1 = TranscriptEvent(
            id = UUID.randomUUID().toString(),
            sessionId = "sess-asr",
            text = "Sir please read those six numbers from the SMS right now."
        )
        val (_, signals1) = semanticEngine.analyze(event1)
        assertTrue(signals1.any { it.tactic == ScamTactic.CREDENTIAL_PRESSURE })

        val event2 = TranscriptEvent(
            id = UUID.randomUUID().toString(),
            sessionId = "sess-asr",
            text = "Tell me the OTB immediately for verification."
        )
        val (_, signals2) = semanticEngine.analyze(event2)
        assertTrue(signals2.any { it.tactic == ScamTactic.CREDENTIAL_PRESSURE })
    }

    @Test
    fun testStrictHardNegativesGuaranteedSafe() {
        val benignPhrases = listOf(
            "Never share your OTP with anyone.",
            "Do not share your PIN or passwords.",
            "Bank employees never ask for your verification code.",
            "My bank sent me an OTP for login.",
            "Why are you shouting at me? I am just a student.",
            "Learn how to avoid digital arrest scams."
        )

        for (phrase in benignPhrases) {
            val event = TranscriptEvent(
                id = UUID.randomUUID().toString(),
                sessionId = "sess-benign",
                text = phrase
            )
            val (_, signals) = semanticEngine.analyze(event)
            assertTrue("False positive detected for safe phrase: '$phrase'", signals.isEmpty())
        }
    }

    @Test
    fun testConversationMemoryCrossTurnPronounResolution() {
        val memory = ConversationMemory()
        val sessId = "sess-memory-001"

        // Turn 1: Mentioning code
        val t1 = TranscriptEvent(id = "1", sessionId = sessId, text = "Did you get the six digit verification code?")
        val (l1, s1) = semanticEngine.analyze(t1)
        memory.recordTurn(t1, l1.primaryLanguage, s1)

        // Turn 2: Vague pronoun "send it immediately"
        val t2 = TranscriptEvent(id = "2", sessionId = sessId, text = "Send it immediately or police will come.")
        val (l2, s2) = semanticEngine.analyze(t2, memory.getPriorTactics(sessId))

        assertTrue(s2.any { it.tactic == ScamTactic.CREDENTIAL_PRESSURE })
        assertEquals("CONTEXT_PRONOUN_RESOLUTION", s2.first { it.tactic == ScamTactic.CREDENTIAL_PRESSURE }.source)
    }

    @Test
    fun testTamperEvidentEvidenceVaultHashChainingAndTamperDetection() {
        val sessId = "sess-vault-001"

        evidenceVault.recordEvent(
            sessionId = sessId,
            eventType = "CALL_INIT",
            source = EvidenceSource.SYSTEM,
            description = "Call started with encrypted stream"
        )
        evidenceVault.recordEvent(
            sessionId = sessId,
            eventType = "TACTIC",
            source = EvidenceSource.MODEL,
            tactic = "CREDENTIAL_PRESSURE",
            description = "Extracted OTP: 849204"
        )

        val events = evidenceVault.getSessionEvidence(sessId)
        assertEquals(2, events.size)

        // Check privacy sanitization: secrets scrubbed
        assertFalse(events[1].description.contains("849204"))
        assertTrue(events[1].description.contains("[REDACTED]"))

        // Check cryptographic hash integrity
        assertTrue(evidenceVault.verifyChainIntegrity(sessId))
    }

    @Test
    fun testVisualThreatDetection() {
        val sample = VideoFrameSample(System.currentTimeMillis(), 1280, 720)

        val anydeskThreat = visualDetector.analyzeFrame(sample, "AnyDesk Remote Support ID: 948 102 391")
        assertNotNull(anydeskThreat)
        assertEquals(VisualThreatType.REMOTE_ACCESS_INTERFACE, anydeskThreat?.type)

        val upiThreat = visualDetector.analyzeFrame(sample, "Enter UPI PIN to approve transfer")
        assertNotNull(upiThreat)
        assertEquals(VisualThreatType.PAYMENT_APP_DETECTED, upiThreat?.type)
    }

    @Test
    fun testEndToEndEscalationToSafetyBrake() {
        val orchestrator = RakshaAIOrchestrator()
        val sessId = "sess-demo-e2e"

        // Step 1: Authority
        val d1 = orchestrator.processTranscript(
            TranscriptEvent(id = "1", sessionId = sessId, text = "Hello, I am calling from Delhi Police Cyber Cell.")
        )
        assertEquals(ScamStage.AUTHORITY, d1.currentStage)
        assertFalse(d1.safetyBrakeTriggered)

        // Step 2: Allegation & Fear
        val d2 = orchestrator.processTranscript(
            TranscriptEvent(id = "2", sessionId = sessId, text = "Your Aadhaar is involved in illegal money laundering case.")
        )
        assertEquals(ScamStage.FEAR, d2.currentStage)

        // Step 3: Isolation
        val d3 = orchestrator.processTranscript(
            TranscriptEvent(id = "3", sessionId = sessId, text = "Close the door, do not tell your family. You are in digital arrest.")
        )
        assertEquals(ScamStage.ISOLATION, d3.currentStage)

        // Step 4: Urgency & Direct Credential/Payment Demand
        val d4 = orchestrator.processTranscript(
            TranscriptEvent(id = "4", sessionId = sessId, text = "Transfer money immediately to secure account and read those six numbers!")
        )
        assertEquals(RiskLevel.CRITICAL, d4.riskLevel)
        assertTrue(d4.safetyBrakeTriggered)
        assertTrue(d4.explanations.isNotEmpty())
        assertTrue(d4.explanations.any { it.contains("OTP/PIN") || it.contains("money transfer") })
    }

    @Test
    fun testProtectionPolicyDefaultsAndCustomization() {
        val controller = ProtectionController()
        val defaultPolicy = controller.policy.value
        assertEquals(ProtectionMode.STRONG_PROTECTION, defaultPolicy.mode)
        assertFalse(defaultPolicy.autoProtectEnabled)
        assertEquals(5, defaultPolicy.countdownSeconds)
        assertEquals(UIMode.ADVANCED, defaultPolicy.uiMode)

        // Customization
        controller.setUIMode(UIMode.SIMPLE_ELDERLY)
        assertEquals(UIMode.SIMPLE_ELDERLY, controller.policy.value.uiMode)

        // Attempting AUTO_PROTECT without explicit user consent
        controller.setProtectionMode(ProtectionMode.AUTO_PROTECT, autoProtectConsent = false)
        assertEquals(ProtectionMode.AUTO_PROTECT, controller.policy.value.mode)
        assertFalse(controller.policy.value.autoProtectEnabled)

        // Enabling with explicit consent
        controller.setProtectionMode(ProtectionMode.AUTO_PROTECT, autoProtectConsent = true)
        assertTrue(controller.policy.value.autoProtectEnabled)
    }

    @Test
    fun testEvidenceStrengthCalculationTiers() {
        val controller = ProtectionController()
        val sessId = "strength-test"

        // Tier 1: WEAK - empty tactics
        val decisionWeak = ProtectionDecision(
            sessionId = sessId,
            riskScore = 10,
            riskLevel = RiskLevel.LOW,
            confidence = 0.5f,
            currentStage = ScamStage.CONTACT,
            manipulationVelocity = 0.1f,
            detectedLanguages = listOf("en"),
            primaryLanguage = "en",
            isCodeSwitching = false,
            detectedTactics = emptyList(),
            safetyBrakeTriggered = false,
            explanations = emptyList()
        )
        assertEquals(EvidenceStrength.WEAK, controller.calculateEvidenceStrength(decisionWeak))

        // Tier 2: MODERATE - 1 tactic
        val decisionModerate = decisionWeak.copy(
            detectedTactics = listOf(
                RiskSignal("1", tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.8f, riskContribution = 20, evidenceText = "police", sessionId = sessId)
            )
        )
        assertEquals(EvidenceStrength.MODERATE, controller.calculateEvidenceStrength(decisionModerate))

        // Tier 3: STRONG - 2 independent categories
        val decisionStrong = decisionWeak.copy(
            detectedTactics = listOf(
                RiskSignal("1", tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.8f, riskContribution = 20, evidenceText = "police", sessionId = sessId),
                RiskSignal("2", tactic = ScamTactic.ISOLATION, confidence = 0.85f, riskContribution = 25, evidenceText = "do not tell", sessionId = sessId)
            )
        )
        assertEquals(EvidenceStrength.STRONG, controller.calculateEvidenceStrength(decisionStrong))

        // Tier 4: VERY_STRONG - 3+ categories and confidence >= 0.85f
        val decisionVeryStrong = decisionWeak.copy(
            confidence = 0.90f,
            detectedTactics = listOf(
                RiskSignal("1", tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.9f, riskContribution = 20, evidenceText = "police", sessionId = sessId),
                RiskSignal("2", tactic = ScamTactic.ISOLATION, confidence = 0.9f, riskContribution = 25, evidenceText = "do not tell", sessionId = sessId),
                RiskSignal("3", tactic = ScamTactic.PAYMENT_DEMAND, confidence = 0.95f, riskContribution = 35, evidenceText = "transfer now", sessionId = sessId)
            )
        )
        assertEquals(EvidenceStrength.VERY_STRONG, controller.calculateEvidenceStrength(decisionVeryStrong))
    }

    @Test
    fun testForceCutSafetyConstraints() {
        var autoTerminated = false
        val controller = ProtectionController(
            initialPolicy = ProtectionPolicy(
                mode = ProtectionMode.STRONG_PROTECTION,
                autoProtectEnabled = false
            ),
            onAutoTerminate = { autoTerminated = true }
        )

        val criticalDecision = ProtectionDecision(
            sessionId = "sess-crit",
            riskScore = 95,
            riskLevel = RiskLevel.CRITICAL,
            confidence = 0.95f,
            currentStage = ScamStage.PAYMENT_CREDENTIAL,
            manipulationVelocity = 0.9f,
            detectedLanguages = listOf("en"),
            primaryLanguage = "en",
            isCodeSwitching = false,
            detectedTactics = listOf(
                RiskSignal("1", tactic = ScamTactic.AUTHORITY_IMPERSONATION, confidence = 0.9f, riskContribution = 20, evidenceText = "cbi", sessionId = "sess-crit"),
                RiskSignal("2", tactic = ScamTactic.ISOLATION, confidence = 0.9f, riskContribution = 25, evidenceText = "don't speak to anyone", sessionId = "sess-crit"),
                RiskSignal("3", tactic = ScamTactic.CREDENTIAL_PRESSURE, confidence = 0.95f, riskContribution = 40, evidenceText = "send otp", sessionId = "sess-crit")
            ),
            safetyBrakeTriggered = true,
            explanations = listOf("Critical credential pressure")
        )

        // In STRONG_PROTECTION mode, auto-protect countdown MUST NOT trigger
        controller.evaluate(criticalDecision)
        assertEquals(ProtectionState.CRITICAL, controller.state.value)
        assertFalse(autoTerminated)

        // Now enable AUTO_PROTECT with explicit user consent
        controller.setProtectionMode(ProtectionMode.AUTO_PROTECT, autoProtectConsent = true)
        controller.evaluate(criticalDecision)
        assertEquals(ProtectionState.PROTECTION_PENDING, controller.state.value)

        // User overrides / cancels the countdown
        controller.cancelCountdownAndKeepCall()
        assertEquals(ProtectionState.USER_OVERRIDE, controller.state.value)
        assertFalse(autoTerminated)

        // Subsequent evaluations must respect USER_OVERRIDE and not trigger countdown again
        controller.evaluate(criticalDecision)
        assertEquals(ProtectionState.USER_OVERRIDE, controller.state.value)
        assertFalse(autoTerminated)

        // User can manually end the call
        controller.endCallNow()
        assertEquals(ProtectionState.CALL_ENDED, controller.state.value)
    }
}
