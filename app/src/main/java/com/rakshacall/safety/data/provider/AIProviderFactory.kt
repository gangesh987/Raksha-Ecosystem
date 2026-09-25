package com.rakshacall.safety.data.provider

import com.rakshacall.safety.data.remote.config.AppConfig
import com.rakshacall.safety.domain.provider.AIProvider

object AIProviderFactory {

    private val localProvider = LocalSafetyProvider()
    private var geminiProvider: GeminiAIProvider? = null
    private var groqProvider: GroqAIProvider? = null

    fun getProvider(
        geminiApiKey: String? = null,
        groqApiKey: String? = null,
        backendBaseUrl: String? = null
    ): AIProvider {
        val groqKey = groqApiKey ?: System.getenv("GROQ_API_KEY")
        if (!groqKey.isNullOrBlank()) {
            if (groqProvider == null) {
                groqProvider = GroqAIProvider(apiKey = groqKey, fallbackLocalProvider = localProvider)
            }
            return groqProvider!!
        }

        val geminiKey = geminiApiKey ?: System.getenv("GEMINI_API_KEY")
        if (!geminiKey.isNullOrBlank()) {
            if (geminiProvider == null) {
                geminiProvider = GeminiAIProvider(
                    apiKey = geminiKey,
                    backendBaseUrl = backendBaseUrl ?: AppConfig.DEFAULT_BASE_URL,
                    fallbackLocalProvider = localProvider
                )
            }
            return geminiProvider!!
        }

        return localProvider
    }

    fun getDeterministicGuardrail(): LocalSafetyProvider = localProvider
}
