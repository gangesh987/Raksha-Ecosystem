package com.rakshacall.safety.data.provider

import com.rakshacall.safety.domain.provider.AIAnalysisResult
import com.rakshacall.safety.domain.provider.AIConnectionState
import com.rakshacall.safety.domain.provider.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString

/**
 * Gemini Live / Gemini API provider implementation.
 * Connects to Gemini for real-time conversational semantic analysis.
 * If credentials are not configured, gracefully marks itself unavailable
 * while the deterministic local safety engine continues uninterrupted.
 */
class GeminiAIProvider(
    private val apiKey: String? = null,
    private val backendBaseUrl: String? = null,
    private val fallbackLocalProvider: LocalSafetyProvider = LocalSafetyProvider()
) : AIProvider {

    override val providerName: String = "Google-Gemini-Safety-Analyst"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override val isAvailable: Boolean
        get() = !apiKey.isNullOrBlank() || !backendBaseUrl.isNullOrBlank()

    override val connectionState: AIConnectionState
        get() = if (isAvailable) AIConnectionState.CONNECTED else AIConnectionState.AI_UNAVAILABLE

    override suspend fun analyze(transcript: String, context: List<String>): AIAnalysisResult = withContext(Dispatchers.IO) {
        if (!isAvailable) {
            val localResult = fallbackLocalProvider.analyze(transcript, context)
            return@withContext localResult.copy(
                modelVersion = "local-deterministic-guardrail (Gemini API key not configured)"
            )
        }

        try {
            if (!apiKey.isNullOrBlank()) {
                analyzeWithDirectGemini(transcript, context)
            } else if (!backendBaseUrl.isNullOrBlank()) {
                analyzeWithBackend(transcript, context)
            } else {
                fallbackLocalProvider.analyze(transcript, context)
            }
        } catch (e: Exception) {
            val local = fallbackLocalProvider.analyze(transcript, context)
            local.copy(
                reason = "AI service temporarily unreachable (${e.localizedMessage ?: "timeout"}). Local guardrail protected: ${local.reason}",
                modelVersion = "local-nlp-fallback"
            )
        }
    }

    private suspend fun analyzeWithDirectGemini(transcript: String, context: List<String>): AIAnalysisResult {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val prompt = """
            You are RakshaCall's real-time scam-risk analyst. Analyze this call transcript segment for coercive digital-arrest scam patterns.
            Conversation/coercion is primary. Return ONLY one compact JSON object, no markdown:
            {
              "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION", "URGENCY", "ISOLATION", "PAYMENT_DEMAND", "CREDENTIAL_PRESSURE", "REMOTE_ACCESS_PRESSURE", "SUSPICIOUS_LINK", "ESCALATION"],
              "stage": "CONTACT|AUTHORITY|FEAR|ISOLATION|DEMAND|PAYMENT_CREDENTIAL|ESCALATION",
              "riskContribution": 0..100,
              "confidence": 0.0..1.0,
              "reason": "short explanation",
              "irreversibleAction": true|false,
              "recommendedAction": "short advice"
            }
            Transcript: "$transcript"
        """.trimIndent()

        val escapedPrompt = json.encodeToString(String.serializer(), prompt)
        val requestBodyJson = """
            {
              "contents": [{
                "parts": [{"text": $escapedPrompt}]
              }],
              "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json"
              }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return fallbackLocalProvider.analyze(transcript, context)
            }
            val body = response.body?.string().orEmpty()
            return parseGeminiResponse(body, transcript, context)
        }
    }

    private suspend fun analyzeWithBackend(transcript: String, context: List<String>): AIAnalysisResult {
        val endpoint = "${backendBaseUrl!!.trimEnd('/')}/api/sessions/1/analyze"
        val escapedTranscript = json.encodeToString(String.serializer(), transcript)
        val payload = """{"transcript":$escapedTranscript,"visual_score":0.15,"liveness_score":0.85}"""
        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return fallbackLocalProvider.analyze(transcript, context)
            }
            val body = response.body?.string().orEmpty()
            val parsed = json.parseToJsonElement(body).jsonObject
            val fusedScore = (parsed["fused_score"]?.jsonPrimitive?.floatOrNull ?: 0.2f) * 100
            val reasons = parsed["reasons"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val irreversible = parsed["irreversible_action"]?.jsonPrimitive?.booleanOrNull ?: false

            
            return AIAnalysisResult(
                tactics = parsed["ai_tactics"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                stage = parsed["ai_stage"]?.jsonPrimitive?.content ?: "CONTACT",
                riskContribution = fusedScore.toInt().coerceIn(0, 100),
                confidence = parsed["ai_confidence"]?.jsonPrimitive?.floatOrNull ?: 0.9f,
                reason = reasons.joinToString("; "),
                irreversibleAction = irreversible,
                recommendedAction = if (irreversible) "PAUSE & VERIFY immediately" else "Monitor call closely",
                modelVersion = "fastapi-gemini-live"
            )
        }
    }

    private suspend fun parseGeminiResponse(responseJson: String, transcript: String, context: List<String>): AIAnalysisResult {
        try {
            val root = json.parseToJsonElement(responseJson).jsonObject
            val candidates = root["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
            val content = firstCandidate?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content.orEmpty()

            val aiObj = json.parseToJsonElement(text).jsonObject
            val tactics = aiObj["tactics"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val stage = aiObj["stage"]?.jsonPrimitive?.content ?: "CONTACT"
            val riskContribution = aiObj["riskContribution"]?.jsonPrimitive?.intOrNull ?: 0
            val confidence = aiObj["confidence"]?.jsonPrimitive?.floatOrNull ?: 0.9f
            val reason = aiObj["reason"]?.jsonPrimitive?.content ?: "Pattern analyzed"
            val irreversible = aiObj["irreversibleAction"]?.jsonPrimitive?.booleanOrNull ?: false
            val action = aiObj["recommendedAction"]?.jsonPrimitive?.content ?: "Verify independently"

            return AIAnalysisResult(
                tactics = tactics,
                stage = stage,
                riskContribution = riskContribution.coerceIn(0, 100),
                confidence = confidence.coerceIn(0f, 1f),
                reason = reason,
                irreversibleAction = irreversible,
                recommendedAction = action,
                modelVersion = "gemini-1.5-flash-direct"
            )
        } catch (e: Exception) {
            return fallbackLocalProvider.analyze(transcript, context)
        }
    }
}
