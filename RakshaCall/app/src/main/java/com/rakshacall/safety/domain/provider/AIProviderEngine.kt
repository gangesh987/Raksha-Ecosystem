package com.rakshacall.safety.domain.provider

import com.rakshacall.safety.data.provider.AIProviderFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-reliability AI provider engine.
 * Implements strict hierarchical fallback: LOCAL -> GROQ -> GEMINI -> SAFE LOCAL FALLBACK.
 * Logs provider failure states transparently and never fabricates responses.
 */
class AIProviderEngine(
    private val localFallback: AIProvider = AIProviderFactory.getDeterministicGuardrail()
) {
    private val _activeProviderName = MutableStateFlow("LOCAL (Deterministic Guardrail)")
    val activeProviderName: StateFlow<String> = _activeProviderName.asStateFlow()

    private val _lastProviderStatus = MutableStateFlow("ACTIVE")
    val lastProviderStatus: StateFlow<String> = _lastProviderStatus.asStateFlow()

    suspend fun analyzeWithFallback(
        transcript: String,
        groqApiKey: String? = null,
        geminiApiKey: String? = null,
        backendBaseUrl: String? = null
    ): AIAnalysisResult {
        val start = System.currentTimeMillis()

        // 1. Try Groq if key is present
        if (!groqApiKey.isNullOrBlank()) {
            try {
                val provider = AIProviderFactory.getProvider(groqApiKey = groqApiKey)
                val result = provider.analyze(transcript)
                if (result.modelVersion.contains("local") || result.reason.contains("error", ignoreCase = true)) {
                    _activeProviderName.value = "LOCAL (Deterministic Fallback)"
                    _lastProviderStatus.value = "AI_PROVIDER_UNAVAILABLE (Groq Error - fallback active)"
                    return result.copy(
                        provider = "LOCAL",
                        latencyMs = System.currentTimeMillis() - start
                    )
                } else {
                    _activeProviderName.value = "GROQ (Cloud LLaMA/Mixtral)"
                    _lastProviderStatus.value = "SUCCESS"
                    return result.copy(
                        provider = "GROQ",
                        latencyMs = System.currentTimeMillis() - start
                    )
                }
            } catch (e: Exception) {
                _lastProviderStatus.value = "AI_PROVIDER_UNAVAILABLE (Groq Error: ${e.message})"
            }
        }

        // 2. Try Gemini if key is present
        if (!geminiApiKey.isNullOrBlank()) {
            try {
                val provider = AIProviderFactory.getProvider(geminiApiKey = geminiApiKey, backendBaseUrl = backendBaseUrl)
                val result = provider.analyze(transcript)
                if (result.modelVersion.contains("local") || result.reason.contains("error", ignoreCase = true)) {
                    _activeProviderName.value = "LOCAL (Deterministic Fallback)"
                    _lastProviderStatus.value = "AI_PROVIDER_UNAVAILABLE (Gemini Error - fallback active)"
                    return result.copy(
                        provider = "LOCAL",
                        latencyMs = System.currentTimeMillis() - start
                    )
                } else {
                    _activeProviderName.value = "GEMINI (Google AI)"
                    _lastProviderStatus.value = "SUCCESS"
                    return result.copy(
                        provider = "GEMINI",
                        latencyMs = System.currentTimeMillis() - start
                    )
                }
            } catch (e: Exception) {
                _lastProviderStatus.value = "AI_PROVIDER_UNAVAILABLE (Gemini Error: ${e.message})"
            }
        }

        // 3. Deterministic Local Safety Fallback (Always operational)
        _activeProviderName.value = "LOCAL (Deterministic Guardrail)"
        val localResult = localFallback.analyze(transcript)
        return localResult.copy(
            provider = "LOCAL",
            latencyMs = System.currentTimeMillis() - start
        )
    }
}
