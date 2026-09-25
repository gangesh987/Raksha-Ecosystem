package com.rakshacall.safety.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.SessionStatus
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.domain.model.VelocityLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class LiveLabUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val sessionStartTime: Long = System.currentTimeMillis(),
    val currentRiskScore: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val currentStage: ScamStage = ScamStage.CONTACT,
    val velocityLevel: VelocityLevel = VelocityLevel.LOW,
    val velocityExplanation: String = "Normal conversational cadence.",
    val transcripts: List<TranscriptEvent> = emptyList(),
    val detectedSignals: List<RiskSignal> = emptyList(),
    val showSafetyBrake: Boolean = false,
    val isMicActive: Boolean = false
)

class LiveInputLabViewModel : ViewModel() {

    private val processTranscriptUseCase = ServiceLocator.processTranscriptUseCase
    private val analyzeRiskUseCase = ServiceLocator.analyzeRiskUseCase
    private val updateScamStageUseCase = ServiceLocator.updateScamStageUseCase
    private val calculateVelocityUseCase = ServiceLocator.calculateManipulationVelocityUseCase
    private val triggerSafetyBrakeUseCase = ServiceLocator.triggerSafetyBrakeUseCase
    private val createEvidenceEventUseCase = ServiceLocator.createEvidenceEventUseCase
    private val sessionRepository = ServiceLocator.sessionRepository
    private val scamStageMachine = ServiceLocator.scamStageMachine

    private val _uiState = MutableStateFlow(LiveLabUiState())
    val uiState: StateFlow<LiveLabUiState> = _uiState.asStateFlow()

    fun resetLab() {
        scamStageMachine.reset()
        _uiState.value = LiveLabUiState()
    }

    fun setMicActive(active: Boolean) {
        _uiState.update { it.copy(isMicActive = active) }
    }

    fun dismissSafetyBrake() {
        _uiState.update { it.copy(showSafetyBrake = false) }
    }

    fun processInput(text: String) {
        if (text.isBlank()) return

        val state = _uiState.value
        viewModelScope.launch {
            val (event, newSignals) = processTranscriptUseCase(
                sessionId = state.sessionId,
                speaker = "CALLER",
                text = text,
                pastSignals = state.detectedSignals
            )

            val updatedSignals = state.detectedSignals + newSignals
            val updatedTranscripts = state.transcripts + event

            var stage = state.currentStage
            for (sig in newSignals) {
                val nextStage = updateScamStageUseCase(sig)
                if (nextStage.order > stage.order) {
                    stage = nextStage
                }
                createEvidenceEventUseCase(
                    sessionId = state.sessionId,
                    eventType = "TACTIC_DETECTED",
                    payloadJson = "{\"tactic\":\"${sig.tactic.name}\",\"weight\":${sig.riskContribution},\"evidence\":\"${sig.evidenceText}\"}"
                )
            }

            val riskDecision = analyzeRiskUseCase(state.sessionId, updatedSignals, state.sessionStartTime)
            val velocity = calculateVelocityUseCase(updatedSignals)
            val isSafetyBrake = triggerSafetyBrakeUseCase(riskDecision.score, updatedSignals)

            // Save session tagged strictly as isDemoSession = true
            sessionRepository.createSession(
                ProtectionSession(
                    id = state.sessionId,
                    startTime = state.sessionStartTime,
                    status = SessionStatus.ACTIVE,
                    peakRisk = riskDecision.score,
                    finalRisk = riskDecision.score,
                    highestStage = stage,
                    inputSource = "LIVE_LAB",
                    safetyBrakeTriggered = isSafetyBrake,
                    totalTacticsDetected = updatedSignals.size,
                    isDemoSession = true // Strict isolation from personal analytics
                )
            )

            _uiState.update {
                it.copy(
                    transcripts = updatedTranscripts,
                    detectedSignals = updatedSignals,
                    currentRiskScore = riskDecision.score,
                    riskLevel = riskDecision.level,
                    currentStage = stage,
                    velocityLevel = velocity.level,
                    velocityExplanation = velocity.explanation,
                    showSafetyBrake = isSafetyBrake || it.showSafetyBrake
                )
            }
        }
    }
}
