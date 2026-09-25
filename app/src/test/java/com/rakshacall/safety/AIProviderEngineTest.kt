package com.rakshacall.safety

import com.rakshacall.safety.domain.provider.AIAnalysisResult
import com.rakshacall.safety.domain.provider.AIConnectionState
import com.rakshacall.safety.domain.provider.AIProvider
import com.rakshacall.safety.domain.provider.AIProviderEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AIProviderEngineTest {

    @Test
    fun testDeterministicLocalFallbackOperatesWithoutCloudKeys() = runTest {
        val engine = AIProviderEngine()
        val result = engine.analyzeWithFallback(
            transcript = "This is Delhi Police. You must transfer money immediately."
        )
        assertEquals("LOCAL", result.provider)
        assertTrue(result.tactics.isNotEmpty())
        assertTrue(result.riskContribution > 0)
        assertEquals("ACTIVE", engine.lastProviderStatus.value)
    }

    @Test
    fun testLatencyMsIsMeasuredAndPopulated() = runTest {
        val engine = AIProviderEngine()
        val result = engine.analyzeWithFallback("Test conversation transcript")
        assertTrue("Latency should be non-negative", result.latencyMs >= 0)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun testProviderMetadataTaggedInResult() = runTest {
        val engine = AIProviderEngine()
        val result = engine.analyzeWithFallback("Tell me your netbanking password and OTP right now.")
        assertEquals("LOCAL", result.provider)
        assertTrue("Should detect credential pressure", result.tactics.contains("CREDENTIAL_PRESSURE"))
        assertTrue("Should flag irreversible action", result.irreversibleAction)
    }

    @Test
    fun testCloudProviderFailureLogsUnavailableAndFallbacksSafely() = runTest {
        // Create an engine with a mock failing cloud provider
        val failingProvider = object : AIProvider {
            override val providerName: String = "MockFailingCloud"
            override val isAvailable: Boolean = false
            override val connectionState: AIConnectionState = AIConnectionState.AI_UNAVAILABLE
            override suspend fun analyze(transcript: String, context: List<String>): AIAnalysisResult {
                throw RuntimeException("Network quota exceeded")
            }
        }
        val engine = AIProviderEngine(localFallback = com.rakshacall.safety.data.provider.LocalSafetyProvider())

        // Pass an invalid groq key to trigger cloud attempt & fallback
        val result = engine.analyzeWithFallback(
            transcript = "Transfer 50,000 rupees",
            groqApiKey = "invalid_key_will_fail"
        )
        // Must fallback to local safely without throwing exception
        assertEquals("LOCAL", result.provider)
        assertTrue(result.riskContribution > 0)
        assertTrue(engine.lastProviderStatus.value.contains("AI_PROVIDER_UNAVAILABLE"))
    }

    @Test
    fun testLocalFallbackHasValidConfidence() = runTest {
        val engine = AIProviderEngine()
        val result = engine.analyzeWithFallback("You are under digital arrest by the CBI")
        assertTrue("Confidence must be separated from risk and > 0.5", result.confidence >= 0.5f)
        assertFalse(result.confidence.isNaN())
    }

    @Test
    fun testRecommendedActionProvidedOnCoercion() = runTest {
        val engine = AIProviderEngine()
        val result = engine.analyzeWithFallback("Do not hang up this call or we will raid your home")
        assertTrue(result.recommendedAction.isNotBlank())
        assertTrue(result.reason.isNotBlank())
    }
}
