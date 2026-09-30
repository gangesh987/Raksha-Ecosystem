package com.rakshacall.safety.intelligence

import com.rakshacall.safety.domain.engine.ManipulationVelocityEngine
import com.rakshacall.safety.domain.engine.ScamStageMachine
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.TranscriptEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class AIExecutionMode {
    LOCAL,
    CLOUD,
    HYBRID,
    DEGRADED
}

data class ProtectionDecision(
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val confidence: Float,
    val currentStage: ScamStage,
    val manipulationVelocity: Float,
    val detectedLanguages: List<String>,
    val primaryLanguage: String,
    val isCodeSwitching: Boolean,
    val detectedTactics: List<RiskSignal>,
    val safetyBrakeTriggered: Boolean,
    val explanations: List<String>,
    val executionMode: AIExecutionMode = AIExecutionMode.HYBRID
)

/**
 * Central AI Orchestrator coordinating multilingual semantic understanding,
 * rolling conversation memory, velocity calculation, scam stages, and protection decisions.
 */
class RakshaAIOrchestrator(
    private val semanticEngine: MultilingualSemanticEngine = MultilingualSemanticEngine(),
    private val memory: ConversationMemory = ConversationMemory(),
    private val velocityEngine: ManipulationVelocityEngine = ManipulationVelocityEngine(),
    private val stageMachine: ScamStageMachine = ScamStageMachine()
) {

    private val _latestDecision = MutableStateFlow<ProtectionDecision?>(null)
    val latestDecision: StateFlow<ProtectionDecision?> = _latestDecision.asStateFlow()

    private var executionMode: AIExecutionMode = AIExecutionMode.HYBRID

    fun setExecutionMode(mode: AIExecutionMode) {
        this.executionMode = mode
    }

    /**
     * Process incoming speech transcript (partial or final).
     */
    fun processTranscript(
        event: TranscriptEvent,
        isFinal: Boolean = true,
        visualRiskBonus: Int = 0
    ): ProtectionDecision {
        // 1. Fetch prior tactics from short-lived conversation memory
        val priorTactics = memory.getPriorTactics(event.sessionId)

        // 2. Multilingual Semantic & Intent Analysis
        val (langAnalysis, newSignals) = semanticEngine.analyze(event, priorTactics)

        // 3. Update Conversation Memory if finalized
        if (isFinal && (newSignals.isNotEmpty() || event.text.isNotBlank())) {
            memory.recordTurn(event, langAnalysis.primaryLanguage, newSignals)
        }

        // 4. Update Scam Stage Machine
        for (sig in newSignals) {
            stageMachine.processSignal(sig)
        }
        val currentStage = stageMachine.getCurrentStage()

        // 5. Calculate Manipulation Velocity
        val allSignals = memory.getPriorTactics(event.sessionId) + newSignals
        val velCalc = velocityEngine.calculateVelocity(allSignals, event.timestamp)
        val velocityFloat = velCalc.weightedScoreInWindow / 20.0f

        // 6. Multi-Tactic Risk Fusion & Score Calculation
        var baseScore = 0
        val uniqueTactics = allSignals.map { it.tactic }.distinct()

        for (t in uniqueTactics) {
            baseScore += t.defaultWeight
        }

        // Synergistic tactic multipliers
        if (uniqueTactics.contains(ScamTactic.AUTHORITY_IMPERSONATION) && uniqueTactics.contains(ScamTactic.CRIMINAL_ALLEGATION)) {
            baseScore += 15
        }
        if (uniqueTactics.contains(ScamTactic.ISOLATION) && uniqueTactics.contains(ScamTactic.PAYMENT_DEMAND)) {
            baseScore += 20
        }
        if (uniqueTactics.contains(ScamTactic.AUTHORITY_IMPERSONATION) && uniqueTactics.contains(ScamTactic.CREDENTIAL_PRESSURE)) {
            baseScore += 25
        }

        // Add visual signals contribution
        baseScore += visualRiskBonus

        // Clamp 0-100
        val fusedScore = baseScore.coerceIn(0, 100)
        val level = RiskLevel.fromScore(fusedScore)

        // Safety Brake triggers on HIGH/CRITICAL or irreversible tactics (OTP, direct payment, remote access)
        val brakeTriggered = fusedScore >= 75 || uniqueTactics.any { it.isIrreversibleAction }

        // 7. Explainable Numbered Reasons (Section 28 & 29)
        val explanations = mutableListOf<String>()
        if (uniqueTactics.contains(ScamTactic.CREDENTIAL_PRESSURE)) {
            explanations.add("Extraction of verification codes (OTP/PIN) detected.")
        }
        if (uniqueTactics.contains(ScamTactic.PAYMENT_DEMAND)) {
            explanations.add("Demand for money transfer or security deposit detected.")
        }
        if (uniqueTactics.contains(ScamTactic.AUTHORITY_IMPERSONATION)) {
            explanations.add("Caller claiming law enforcement or regulatory authority.")
        }
        if (uniqueTactics.contains(ScamTactic.ISOLATION)) {
            explanations.add("Social isolation instructions (demanding secrecy/closed room).")
        }
        if (uniqueTactics.contains(ScamTactic.REMOTE_ACCESS)) {
            explanations.add("Instruction to install remote access or screen-sharing application.")
        }
        if (velCalc.level == com.rakshacall.safety.domain.model.VelocityLevel.HIGH) {
            explanations.add(velCalc.explanation)
        }
        if (explanations.isEmpty()) {
            explanations.add("Normal conversation flow.")
        }

        val numberedExplanations = explanations.mapIndexed { idx, reason -> "${idx + 1}. $reason" }

        val decision = ProtectionDecision(
            sessionId = event.sessionId,
            timestamp = event.timestamp,
            riskScore = fusedScore,
            riskLevel = level,
            confidence = langAnalysis.confidence,
            currentStage = currentStage,
            manipulationVelocity = velocityFloat,
            detectedLanguages = langAnalysis.detectedLanguages,
            primaryLanguage = langAnalysis.primaryLanguage,
            isCodeSwitching = langAnalysis.isCodeSwitching,
            detectedTactics = allSignals,
            safetyBrakeTriggered = brakeTriggered,
            explanations = numberedExplanations,
            executionMode = executionMode
        )

        _latestDecision.value = decision
        memory.updateStageAndRisk(event.sessionId, currentStage, fusedScore)

        return decision
    }

    fun endSession(sessionId: String) {
        memory.clearSession(sessionId)
        stageMachine.reset()
        _latestDecision.value = null
    }
}
