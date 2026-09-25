package com.rakshacall.safety.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshacall.safety.core.speech.SpeechLanguageManager
import com.rakshacall.safety.core.speech.SupportedLanguage
import com.rakshacall.safety.data.remote.config.AppConfig
import com.rakshacall.safety.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val lowThreshold: Int = 30,
    val highThreshold: Int = 60,
    val criticalThreshold: Int = 80,
    val activeLanguage: SupportedLanguage = SpeechLanguageManager.getActiveLanguage(),
    val availableLanguages: List<SupportedLanguage> = SpeechLanguageManager.availableLanguages,
    val isCloudSyncEnabled: Boolean = AppConfig.isCloudSyncEnabled
)

class SettingsViewModel : ViewModel() {

    private val preferences = ServiceLocator.preferences

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.lowThreshold.collect { low ->
                _uiState.update { it.copy(lowThreshold = low) }
            }
        }
        viewModelScope.launch {
            preferences.highThreshold.collect { high ->
                _uiState.update { it.copy(highThreshold = high) }
            }
        }
        viewModelScope.launch {
            preferences.criticalThreshold.collect { crit ->
                _uiState.update { it.copy(criticalThreshold = crit) }
            }
        }
    }

    fun updateThresholds(low: Int, high: Int, crit: Int) {
        viewModelScope.launch {
            preferences.updateThresholds(low, high, crit)
            ServiceLocator.riskEngine.lowThreshold = low
            ServiceLocator.riskEngine.highThreshold = high
            ServiceLocator.riskEngine.criticalThreshold = crit
        }
    }

    fun setLanguage(code: String) {
        SpeechLanguageManager.activeLanguageCode = code
        _uiState.update { it.copy(activeLanguage = SpeechLanguageManager.getActiveLanguage()) }
    }

    fun toggleCloudSync(enabled: Boolean) {
        AppConfig.isCloudSyncEnabled = enabled
        _uiState.update { it.copy(isCloudSyncEnabled = enabled) }
    }
}
