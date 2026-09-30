package com.rakshacall.safety.intelligence

import com.rakshacall.safety.domain.engine.ManipulationVelocityEngine
import com.rakshacall.safety.domain.engine.ScamStageMachine
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.intelligence.behaviour.BehaviourMetrics
import com.rakshacall.safety.intelligence.behaviour.BehaviourTurn
import com.rakshacall.safety.intelligence.behaviour.BehaviouralAIEngine
import com.rakshacall.safety.intelligence.graph.EvidenceGraph
import com.rakshacall.safety.intelligence.graph.EvidenceNodeType
import com.rakshacall.safety.intelligence.intent.EntityExtractionEngine
import com.rakshacall.safety.intelligence.intent.ExtractedEntity
import com.rakshacall.safety.intelligence.intent.InferredIntent
import com.rakshacall.safety.intelligence.intent.OpenVocabularyIntentEngine
import com.rakshacall.safety.intelligence.risk.AudioRisk
import com.rakshacall.safety.intelligence.risk.BehaviourRisk
import com.rakshacall.safety.intelligence.risk.ConfidenceEngine
import com.rakshacall.safety.intelligence.risk.ContextRisk
import com.rakshacall.safety.intelligence.risk.RiskFusionEngine2
import com.rakshacall.safety.intelligence.risk.SemanticRisk
import com.rakshacall.safety.intelligence.risk.StageRisk
import com.rakshacall.safety.intelligence.risk.VisualRisk
import com.rakshacall.safety.intelligence.router.ModelRouter
import com.rakshacall.safety.intelligence.router.RoutingDecision
import com.rakshacall.safety.protection.EvidenceStrength
import com.rakshacall.safety.intelligence.audio.AcousticMetrics
import com.rakshacall.safety.intelligence.visual.VisualThreatSignal
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
    val executionMode: AIExecutionMode = AIExecutionMode.HYBRID,
    val inferredIntent: InferredIntent = InferredIntent.BENIGN_INQUIRY,
    val extractedEntities: List<ExtractedEntity> = emptyList(),
    val behaviourMetrics: BehaviourMetrics = BehaviourMetrics(),
    val evidenceStrength: EvidenceStrength = EvidenceStrength.WEAK,
    val sessionSummary: String = ""
)

/**
 * Real-Time Multimodal Autonomous Safety Orchestrator (Phase 1).
 * Coordinates:
 * - Multilingual semantic analysis across 8 languages
 * - Open-vocabulary intent inference
 * - Entity extraction
 * - Behavioural AI & psychological pressure tracking
 * - Evidence graph causal construction
 * - Risk Engine 2.0 multi-domain fusion
 * - Orthogonal Confidence Engine
 * - Model routing & graceful degradation
 */
class RakshaAIOrchestrator(
    private val semanticEngine: MultilingualSemanticEngine = MultilingualSemanticEngine(),
    private val memory: ConversationMemory = ConversationMemory(),
    private val velocityEngine: ManipulationVelocityEngine = ManipulationVelocityEngine(),
    private val stageMachine: ScamStageMachine = ScamStageMachine(),
    private val intentEngine: OpenVocabularyIntentEngine = OpenVocabularyIntentEngine(),
    private val entityEngine: EntityExtractionEngine = EntityExtractionEngine(),
    private val behaviouralEngine: BehaviouralAIEngine = BehaviouralAIEngine(),
    val evidenceGraph: EvidenceGraph = EvidenceGraph(),
    private val confidenceEngine: ConfidenceEngine = ConfidenceEngine(),
    private val riskFusionEngine2: RiskFusionEngine2 = RiskFusionEngine2(),
    val modelRouter: ModelRouter = ModelRouter()
) {

    private val _latestDecision = MutableStateFlow<ProtectionDecision?>(null)
    val latestDecision: StateFlow<ProtectionDecision?> = _latestDecision.asStateFlow()

    private var executionMode: AIExecutionMode = AIExecutionMode.HYBRID
    @Volatile private var latestAcousticMetrics: AcousticMetrics? = null

    fun setExecutionMode(mode: AIExecutionMode) {
        this.executionMode = mode
    }

    fun updateAcousticMetrics(metrics: AcousticMetrics) {
        this.latestAcousticMetrics = metrics
    }

    fun getLatestAcousticMetrics(): AcousticMetrics? = latestAcousticMetrics

    fun processVisualThreat(threat: VisualThreatSignal, sessionId: String): ProtectionDecision? {
        evidenceGraph.addNode(
            EvidenceNodeType.VISUAL_EVENT,
            threat.description,
            weight = 1.2f
        )
        val current = _latestDecision.value
        if (current != null) {
            val updatedScore = (current.riskScore + threat.riskScoreBonus).coerceIn(0, 100)
            val updated = current.copy(
                riskScore = updatedScore,
                riskLevel = RiskLevel.fromScore(updatedScore),
                explanations = (current.explanations + "Visual threat detected: ${threat.description}").distinct()
            )
            _latestDecision.value = updated
            return updated
        }
        return null
    }

    /**
     * Process incoming speech transcript through the end-to-end pipeline.
     */
    fun processTranscript(
        event: TranscriptEvent,
        isFinal: Boolean = true,
        visualRiskBonus: Int = 0,
        visualThreatCount: Int = 0
    ): ProtectionDecision {
        // 1. Fetch prior tactics from short-lived rolling memory
        val priorTactics = memory.getPriorTactics(event.sessionId)

        // 2. Multilingual Semantic & Language ID Analysis
        val (langAnalysis, newSignals) = semanticEngine.analyze(event, priorTactics)

        // 3. Open-Vocabulary Intent Inference (Phase 7)
        val intentResult = intentEngine.inferIntent(event.text, event.speaker)

        // 4. Entity Extraction (Phase 9)
        val entities = entityEngine.extractEntities(event.text)

        // 5. Update Conversation Memory if finalized
        if (isFinal && (newSignals.isNotEmpty() || event.text.isNotBlank())) {
            val sessionMem = memory.recordTurn(event, langAnalysis.primaryLanguage, newSignals)
            sessionMem.cumulativeEntities.addAll(entities.map { it.type })
        }

        // 6. Behavioural AI & Pressure Tracking (Phase 10)
        behaviouralEngine.recordTurn(
            BehaviourTurn(
                timestamp = event.timestamp,
                intent = intentResult.intent,
                tactics = newSignals.map { it.tactic },
                speaker = event.speaker
            )
        )
        val behaviourMetrics = behaviouralEngine.calculateMetrics()

        // 7. Update Live Evidence Graph (Phase 13)
        when (intentResult.intent) {
            InferredIntent.POSSIBLE_ARREST_THREAT -> evidenceGraph.addNode(EvidenceNodeType.THREAT, "Arrest threat: ${event.text}")
            InferredIntent.POSSIBLE_VERIFICATION_CODE_REQUEST -> evidenceGraph.addNode(EvidenceNodeType.CREDENTIAL_REQUEST, "Credential request: ${event.text}")
            InferredIntent.POSSIBLE_FINANCIAL_TRANSFER_REQUEST -> evidenceGraph.addNode(EvidenceNodeType.PAYMENT_REQUEST, "Payment transfer: ${event.text}")
            InferredIntent.POSSIBLE_REMOTE_ACCESS_REQUEST -> evidenceGraph.addNode(EvidenceNodeType.REMOTE_ACCESS, "Remote access demand: ${event.text}")
            InferredIntent.POSSIBLE_ISOLATION_DEMAND -> evidenceGraph.addNode(EvidenceNodeType.ISOLATION, "Isolation demand: ${event.text}")
            InferredIntent.POSSIBLE_IDENTITY_COERCION -> evidenceGraph.addNode(EvidenceNodeType.CALLER_CLAIM, "Coercive authority claim: ${event.text}")
            else -> {}
        }
        if (visualThreatCount > 0) {
            evidenceGraph.addNode(EvidenceNodeType.VISUAL_EVENT, "Visual screen threat detected", weight = 1.2f)
        }

        // 8. Update Scam Stage Machine
        for (sig in newSignals) {
            stageMachine.processSignal(sig)
        }
        val currentStage = stageMachine.getCurrentStage()

        // 9. Calculate Manipulation Velocity
        val allSignals = memory.getPriorTactics(event.sessionId) + newSignals
        val velCalc = velocityEngine.calculateVelocity(allSignals, event.timestamp)
        val velocityFloat = velCalc.weightedScoreInWindow / 20.0f

        // 10. Multi-Domain Risk Decomposition (Phase 11)
        var semanticBaseScore = 0
        val uniqueTactics = allSignals.map { it.tactic }.distinct()
        for (t in uniqueTactics) {
            semanticBaseScore += t.defaultWeight
        }
        // Incorporate open-vocabulary intent weight
        semanticBaseScore += intentResult.intent.riskWeight

        // Synergistic tactic multipliers
        if (uniqueTactics.contains(ScamTactic.AUTHORITY_IMPERSONATION) && uniqueTactics.contains(ScamTactic.CRIMINAL_ALLEGATION)) {
            semanticBaseScore += 15
        }
        if (uniqueTactics.contains(ScamTactic.ISOLATION) && uniqueTactics.contains(ScamTactic.PAYMENT_DEMAND)) {
            semanticBaseScore += 20
        }
        if (uniqueTactics.contains(ScamTactic.AUTHORITY_IMPERSONATION) && uniqueTactics.contains(ScamTactic.CREDENTIAL_PRESSURE)) {
            semanticBaseScore += 25
        }

        val semanticRisk = SemanticRisk(
            score = semanticBaseScore.coerceIn(0, 100),
            tactics = uniqueTactics,
            primaryIntent = intentResult.intent
        )

        val behaviourRisk = BehaviourRisk(
            score = (behaviourMetrics.overallBehaviouralRisk * 100).toInt().coerceIn(0, 100),
            metrics = behaviourMetrics
        )

        val visualRisk = VisualRisk(
            score = visualRiskBonus.coerceIn(0, 100),
            threatCount = visualThreatCount,
            description = if (visualThreatCount > 0) "Remote interface or banking overlay" else null
        )

        val audioRisk = AudioRisk(
            score = when {
                latestAcousticMetrics != null && latestAcousticMetrics!!.isSpeech && latestAcousticMetrics!!.speechActivityRate > 0.65f && latestAcousticMetrics!!.spectralCentroid > 2200f -> 45
                latestAcousticMetrics != null && latestAcousticMetrics!!.isSpeech && latestAcousticMetrics!!.speechActivityRate > 0.4f -> 25
                behaviourMetrics.urgencyPressure > 0.5f -> 35
                else -> 10
            }
        )

        val contextRisk = ContextRisk(
            score = if (entities.any { it.type == com.rakshacall.safety.intelligence.intent.EntityType.OTP || it.type == com.rakshacall.safety.intelligence.intent.EntityType.BANK }) 30 else 5
        )

        val stageRisk = StageRisk(
            score = (currentStage.order * 15).coerceIn(0, 100),
            stage = currentStage
        )

        // 11. Orthogonal Confidence Engine (Phase 12)
        val isSafePhrase = intentResult.intent == InferredIntent.SAFE_ADVICE_OR_DEFENSE
        val distinctSources = (if (uniqueTactics.isNotEmpty()) 1 else 0) +
                (if (visualThreatCount > 0) 1 else 0) +
                (if (behaviourMetrics.manipulationVelocity > 0.3f) 1 else 0)

        val confidenceAssessment = confidenceEngine.evaluateConfidence(
            asrConfidence = langAnalysis.confidence,
            tacticsCount = uniqueTactics.size,
            distinctSourceCount = distinctSources.coerceAtLeast(1),
            graphCorroborationScore = evidenceGraph.calculateCorroborationScore(),
            hasVisualCorroboration = visualThreatCount > 0,
            isHardNegativeSafe = isSafePhrase
        )

        // 12. Risk Fusion Engine 2.0 (Phase 11)
        val fused = riskFusionEngine2.fuse(
            semantic = semanticRisk,
            behaviour = behaviourRisk,
            visual = visualRisk,
            audio = audioRisk,
            context = contextRisk,
            stage = stageRisk,
            confidenceAssessment = confidenceAssessment
        )

        // 13. Safety Brake Trigger
        val brakeTriggered = fused.smoothedRisk >= 75 || uniqueTactics.any { it.isIrreversibleAction } || intentResult.intent.isCoercive && intentResult.intent.riskWeight >= 35

        // 14. Explainable Explanations
        val explanations = mutableListOf<String>()
        if (uniqueTactics.contains(ScamTactic.CREDENTIAL_PRESSURE) || intentResult.intent == InferredIntent.POSSIBLE_VERIFICATION_CODE_REQUEST) {
            explanations.add("Extraction of verification codes (OTP/PIN) detected.")
        }
        if (uniqueTactics.contains(ScamTactic.PAYMENT_DEMAND) || intentResult.intent == InferredIntent.POSSIBLE_FINANCIAL_TRANSFER_REQUEST) {
            explanations.add("Demand for money transfer or security deposit detected.")
        }
        if (uniqueTactics.contains(ScamTactic.AUTHORITY_IMPERSONATION) || intentResult.intent == InferredIntent.POSSIBLE_IDENTITY_COERCION) {
            explanations.add("Caller claiming law enforcement or regulatory authority.")
        }
        if (uniqueTactics.contains(ScamTactic.ISOLATION) || intentResult.intent == InferredIntent.POSSIBLE_ISOLATION_DEMAND) {
            explanations.add("Social isolation instructions (demanding secrecy/closed room).")
        }
        if (uniqueTactics.contains(ScamTactic.REMOTE_ACCESS) || intentResult.intent == InferredIntent.POSSIBLE_REMOTE_ACCESS_REQUEST) {
            explanations.add("Instruction to install remote access or screen-sharing application.")
        }
        if (velCalc.level == com.rakshacall.safety.domain.model.VelocityLevel.HIGH) {
            explanations.add(velCalc.explanation)
        }
        if (explanations.isEmpty()) {
            explanations.add(if (isSafePhrase) "Safe defensive security context." else "Normal conversation flow.")
        }

        val numberedExplanations = explanations.mapIndexed { idx, reason -> "${idx + 1}. $reason" }
        val sessionSummary = memory.generateSessionSummary(event.sessionId)

        val decision = ProtectionDecision(
            sessionId = event.sessionId,
            timestamp = event.timestamp,
            riskScore = fused.smoothedRisk,
            riskLevel = fused.riskLevel,
            confidence = confidenceAssessment.score,
            currentStage = currentStage,
            manipulationVelocity = velocityFloat,
            detectedLanguages = langAnalysis.detectedLanguages,
            primaryLanguage = langAnalysis.primaryLanguage,
            isCodeSwitching = langAnalysis.isCodeSwitching,
            detectedTactics = allSignals,
            safetyBrakeTriggered = brakeTriggered,
            explanations = numberedExplanations,
            executionMode = executionMode,
            inferredIntent = intentResult.intent,
            extractedEntities = entities,
            behaviourMetrics = behaviourMetrics,
            evidenceStrength = confidenceAssessment.evidenceStrength,
            sessionSummary = sessionSummary
        )

        _latestDecision.value = decision
        memory.updateStageAndRisk(event.sessionId, currentStage, fused.smoothedRisk)

        return decision
    }

    fun endSession(sessionId: String) {
        memory.clearSession(sessionId)
        stageMachine.reset()
        evidenceGraph.clear()
        behaviouralEngine.reset()
        riskFusionEngine2.reset()
        _latestDecision.value = null
    }
}
