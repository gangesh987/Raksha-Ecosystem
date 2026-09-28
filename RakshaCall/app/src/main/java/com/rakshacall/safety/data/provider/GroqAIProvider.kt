package com.rakshacall.safety.data.provider

import com.rakshacall.safety.domain.provider.AIAnalysisResult
import com.rakshacall.safety.domain.provider.AIConnectionState
import com.rakshacall.safety.domain.provider.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * High-speed AI Provider powered by Groq cloud inference.
 * Provides sub-300ms structured scam classification with automatic fallback
 * to the deterministic local guardrail.
 */
class GroqAIProvider(
    private val apiKey: String? = null,
    private val model: String = "openai/gpt-oss-20b",
    private val fallbackLocalProvider: LocalSafetyProvider = LocalSafetyProvider()
) : AIProvider {

    override val providerName: String = "Groq-LLM ($model)"

    override val isAvailable: Boolean
        get() = !apiKey.isNullOrBlank()

    override val connectionState: AIConnectionState
        get() = if (isAvailable) AIConnectionState.CONNECTED else AIConnectionState.AI_UNAVAILABLE

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    override suspend fun analyze(transcript: String, context: List<String>): AIAnalysisResult = withContext(Dispatchers.IO) {
        if (!isAvailable) {
            val local = fallbackLocalProvider.analyze(transcript, context)
            return@withContext local.copy(
                modelVersion = "local-deterministic-guardrail (Groq API key not configured)"
            )
        }

        try {
            val systemPrompt = """
                You are RakshaCall's real-time scam-risk analyst. Analyze caller conversation content only for coercive scam tactics.
                Return ONLY one compact JSON object with keys:
                {
                  "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION", "URGENCY", "ISOLATION", "PAYMENT_DEMAND", "CREDENTIAL_PRESSURE", "REMOTE_ACCESS_PRESSURE", "SUSPICIOUS_LINK", "ESCALATION"],
                  "stage": "CONTACT|AUTHORITY|FEAR|ISOLATION|DEMAND|PAYMENT_CREDENTIAL|ESCALATION",
                  "riskContribution": 0..100,
                  "confidence": 0.0..1.0,
                  "reason": "short explanation",
                  "irreversibleAction": true|false,
                  "recommendedAction": "short guidance sentence"
                }
            """.trimIndent()

            val escapedSystem = json.encodeToString(String.serializer(), systemPrompt)
            val escapedTranscript = json.encodeToString(String.serializer(), "Analyze conversation: $transcript")

            val requestBodyJson = """
                {
                  "model": "$model",
                  "response_format": {"type": "json_object"},
                  "messages": [
                    {"role": "system", "content": $escapedSystem},
                    {"role": "user", "content": $escapedTranscript}
                  ]
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (RakshaCall-Android-Sentinel/3.0)")
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val local = fallbackLocalProvider.analyze(transcript, context)
                    return@withContext local.copy(
                        reason = "Groq HTTP ${response.code}; local guardrail protected: ${local.reason}",
                        modelVersion = "local-nlp-fallback"
                    )
                }

                val body = response.body?.string().orEmpty()
                val root = json.parseToJsonElement(body).jsonObject
                val choices = root["choices"]?.jsonArray
                val firstChoice = choices?.firstOrNull()?.jsonObject
                val message = firstChoice?.get("message")?.jsonObject
                val content = message?.get("content")?.jsonPrimitive?.content.orEmpty()

                val aiObj = json.parseToJsonElement(content).jsonObject
                val tactics = aiObj["tactics"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val stage = aiObj["stage"]?.jsonPrimitive?.content ?: "CONTACT"
                val riskContribution = aiObj["riskContribution"]?.jsonPrimitive?.intOrNull ?: 0
                val confidence = aiObj["confidence"]?.jsonPrimitive?.floatOrNull ?: 0.9f
                val reason = aiObj["reason"]?.jsonPrimitive?.content ?: "Groq semantic analysis"
                val irreversible = aiObj["irreversibleAction"]?.jsonPrimitive?.booleanOrNull ?: false
                val action = aiObj["recommendedAction"]?.jsonPrimitive?.content ?: "Verify independently"

                AIAnalysisResult(
                    tactics = tactics,
                    stage = stage,
                    riskContribution = riskContribution.coerceIn(0, 100),
                    confidence = confidence.coerceIn(0f, 1f),
                    reason = reason,
                    irreversibleAction = irreversible,
                    recommendedAction = action,
                    modelVersion = "groq-$model"
                )
            }
        } catch (e: Exception) {
            val local = fallbackLocalProvider.analyze(transcript, context)
            local.copy(
                reason = "Groq service error (${e.localizedMessage ?: "timeout"}). Local guardrail protected: ${local.reason}",
                modelVersion = "local-nlp-fallback"
            )
        }
    }
}
