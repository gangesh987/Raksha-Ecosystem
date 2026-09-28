package com.raksha.video.protection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProtectionViewModel(app: Application) : AndroidViewModel(app) {
    private val client = RakshaProtectionClient()
    val state = client.observeState()
    val riskEvents = client.observeRiskEvents()
    val timeline = client.observeTimeline()
    private val _latestRisk = MutableStateFlow<RiskEvent?>(null)
    val latestRisk = _latestRisk.asStateFlow()
    init { viewModelScope.launch { riskEvents.collect { _latestRisk.value = it } } }
    fun enable(callId: String, consent: ConsentState) { viewModelScope.launch { client.attachToCall(callId, consent) } }
    fun send(event: ProtectionEvent) { viewModelScope.launch { client.sendEvent(event) } }
    fun submitTranscript(text: String, speaker: String = "UNKNOWN", confidence: Float? = null) { viewModelScope.launch { client.submitTranscript(text, speaker, confidence) } }
    fun submitVisualSignal(type: String, confidence: Float? = null, metadata: Map<String, Any?> = emptyMap()) { viewModelScope.launch { client.submitVisualSignal(type, confidence, metadata) } }
    fun disable() { viewModelScope.launch { client.detachFromCall() } }
    override fun onCleared() { client.close() }
}
