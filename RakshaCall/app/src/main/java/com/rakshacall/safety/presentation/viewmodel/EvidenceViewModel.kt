package com.rakshacall.safety.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshacall.safety.core.security.IntegrityResult
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.ProtectionSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EvidenceUiState(
    val sessions: List<ProtectionSession> = emptyList(),
    val selectedSession: ProtectionSession? = null,
    val evidenceEvents: List<EvidenceEvent> = emptyList(),
    val integrityResult: IntegrityResult = IntegrityResult.Empty,
    val isVerifying: Boolean = false
)

class EvidenceViewModel : ViewModel() {

    private val sessionRepository = ServiceLocator.sessionRepository
    private val evidenceRepository = ServiceLocator.evidenceRepository
    private val verifyIntegrityUseCase = ServiceLocator.verifyEvidenceIntegrityUseCase

    private val _uiState = MutableStateFlow(EvidenceUiState())
    val uiState: StateFlow<EvidenceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.observeAllSessions().collect { list ->
                _uiState.update { it.copy(sessions = list) }
            }
        }
    }

    fun selectSession(session: ProtectionSession) {
        _uiState.update { it.copy(selectedSession = session, isVerifying = true) }
        viewModelScope.launch {
            evidenceRepository.observeEvidence(session.id).collect { events ->
                val result = verifyIntegrityUseCase(session.id)
                _uiState.update {
                    it.copy(
                        evidenceEvents = events,
                        integrityResult = result,
                        isVerifying = false
                    )
                }
            }
        }
    }

    fun reVerifyIntegrity(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true) }
            val result = verifyIntegrityUseCase(sessionId)
            _uiState.update { it.copy(integrityResult = result, isVerifying = false) }
        }
    }
}
