package com.rakshacall.safety.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.domain.model.VelocityLevel
import com.rakshacall.safety.domain.model.VisualSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ProtectionUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val sessionStartTime: Long = System.currentTimeMillis(),
    val currentRiskScore: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val currentStage: ScamStage = ScamStage.CONTACT,
    val velocityLevel: VelocityLevel = VelocityLevel.LOW,
    val velocityExplanation: String = "Normal conversational pace.",
    val transcripts: List<TranscriptEvent> = emptyList(),
    val detectedSignals: List<RiskSignal> = emptyList(),
    val showSafetyBrake: Boolean = false,
    val isMicActive: Boolean = false,
    val visualSignal: VisualSignal? = null,
    val disagreementExplanation: String? = null
)

class ProtectionViewModel : ViewModel() {

    private val processTranscriptUseCase = ServiceLocator.processTranscriptUseCase
    private val analyzeRiskUseCase = ServiceLocator.analyzeRiskUseCase
    private val updateScamStageUseCase = ServiceLocator.updateScamStageUseCase
    private val calculateVelocityUseCase = ServiceLocator.calculateManipulationVelocityUseCase
    private val triggerSafetyBrakeUseCase = ServiceLocator.triggerSafetyBrakeUseCase
    private val createEvidenceEventUseCase = ServiceLocator.createEvidenceEventUseCase
    private val fuseRiskSignalsUseCase = ServiceLocator.fuseRiskSignalsUseCase
    private val sessionRepository = ServiceLocator.sessionRepository

    private val _uiState = MutableStateFlow(ProtectionUiState())
    val uiState: StateFlow<ProtectionUiState> = _uiState.asStateFlow()

    fun initializeSession(sessionId: String) {
        _uiState.update {
            it.copy(
                sessionId = sessionId,
                sessionStartTime = System.currentTimeMillis(),
                currentRiskScore = 0,
                riskLevel = RiskLevel.LOW,
                currentStage = ScamStage.CONTACT,
                transcripts = emptyList(),
                detectedSignals = emptyList(),
                showSafetyBrake = false
            )
        }
    }

    fun processSpeechChunk(text: String, speaker: String = "CALLER") {
        if (text.isBlank()) return

        val state = _uiState.value
        viewModelScope.launch {
            val (transcriptEvent, newSignals) = processTranscriptUseCase(
                sessionId = state.sessionId,
                speaker = speaker,
                text = text,
                pastSignals = state.detectedSignals
            )

            val updatedSignals = state.detectedSignals + newSignals
            val updatedTranscripts = state.transcripts + transcriptEvent

            // Evaluate stage transition for each new signal
            var highestStage = state.currentStage
            for (sig in newSignals) {
                val nextStage = updateScamStageUseCase(sig)
                if (nextStage.order > highestStage.order) {
                    highestStage = nextStage
                }
                // Append SHA-256 evidence event
                createEvidenceEventUseCase(
                    sessionId = state.sessionId,
                    eventType = "TACTIC_DETECTED",
                    payloadJson = "{\"tactic\":\"${sig.tactic.name}\",\"weight\":${sig.riskContribution},\"evidence\":\"${sig.evidenceText}\"}"
                )
            }

            // Calculate risk decision & trajectory
            val riskDecision = analyzeRiskUseCase(state.sessionId, updatedSignals, state.sessionStartTime)
            val velocityCalc = calculateVelocityUseCase(updatedSignals)
            val isSafetyBrake = triggerSafetyBrakeUseCase(riskDecision.score, updatedSignals)

            val fused = fuseRiskSignalsUseCase(
                speechRiskScore = riskDecision.score,
                scamStage = highestStage,
                signals = updatedSignals,
                visualSignal = state.visualSignal,
                isSafetyBrakeTriggered = isSafetyBrake
            )

            _uiState.update {
                it.copy(
                    transcripts = updatedTranscripts,
                    detectedSignals = updatedSignals,
                    currentRiskScore = riskDecision.score,
                    riskLevel = riskDecision.level,
                    currentStage = highestStage,
                    velocityLevel = velocityCalc.level,
                    velocityExplanation = velocityCalc.explanation,
                    showSafetyBrake = isSafetyBrake || it.showSafetyBrake,
                    disagreementExplanation = fused.disagreementExplanation
                )
            }
        }
    }

    fun dismissSafetyBrake() {
        _uiState.update { it.copy(showSafetyBrake = false) }
    }

    fun endCurrentSession() {
        val state = _uiState.value
        viewModelScope.launch {
            sessionRepository.endSession(
                sessionId = state.sessionId,
                finalRisk = state.currentRiskScore,
                peakRisk = state.currentRiskScore
            )
        }
    }
}
