package com.rakshacall.safety.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.ProtectionSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IntelligenceUiState(
    val totalSessions: Int = 0,
    val averagePeakRisk: Int = 0,
    val criticalIncidentsCount: Int = 0,
    val highRiskIncidentsCount: Int = 0,
    val stageDistribution: Map<String, Int> = emptyMap(),
    val nonDemoSessions: List<ProtectionSession> = emptyList(),
    val isEmpty: Boolean = true
)

class IntelligenceViewModel : ViewModel() {

    private val sessionRepository = ServiceLocator.sessionRepository

    private val _uiState = MutableStateFlow(IntelligenceUiState())
    val uiState: StateFlow<IntelligenceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.observeAllSessions().collect { allSessions ->
                // Filter out demo sessions to maintain pristine personal analytics
                val userSessions = allSessions.filter { !it.isDemoSession }

                if (userSessions.isEmpty()) {
                    _uiState.value = IntelligenceUiState(isEmpty = true)
                } else {
                    val total = userSessions.size
                    val avgPeak = userSessions.map { it.peakRisk }.average().toInt()
                    val critical = userSessions.count { it.peakRisk >= 80 }
                    val high = userSessions.count { it.peakRisk in 60..79 }
                    val stages = userSessions.groupBy { it.highestStage.displayName }
                        .mapValues { it.value.size }

                    _uiState.value = IntelligenceUiState(
                        totalSessions = total,
                        averagePeakRisk = avgPeak,
                        criticalIncidentsCount = critical,
                        highRiskIncidentsCount = high,
                        stageDistribution = stages,
                        nonDemoSessions = userSessions,
                        isEmpty = false
                    )
                }
            }
        }
    }
}
