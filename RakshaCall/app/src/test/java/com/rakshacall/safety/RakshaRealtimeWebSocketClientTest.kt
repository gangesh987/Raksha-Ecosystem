package com.rakshacall.safety

import com.rakshacall.safety.data.remote.realtime.RakshaRealtimeWebSocketClient
import com.rakshacall.safety.data.remote.realtime.RealtimeRiskUpdate
import com.rakshacall.safety.domain.provider.AIConnectionState
import org.junit.Assert.*
import org.junit.Test

class RakshaRealtimeWebSocketClientTest {

    @Test
    fun testBuildWebSocketUrlFormatsCorrectly() {
        // Plain HTTP emulator URL
        val emulatorUrl = RakshaRealtimeWebSocketClient.buildWebSocketUrl("http://10.0.2.2:8000", 123L)
        assertEquals("ws://10.0.2.2:8000/api/ws/sessions/123", emulatorUrl)

        // Trailing slash
        val trailingSlashUrl = RakshaRealtimeWebSocketClient.buildWebSocketUrl("http://10.0.2.2:8000/", 123L)
        assertEquals("ws://10.0.2.2:8000/api/ws/sessions/123", trailingSlashUrl)

        // HTTPS production URL converts to WSS
        val prodUrl = RakshaRealtimeWebSocketClient.buildWebSocketUrl("https://api.rakshacall.org", 456L)
        assertEquals("wss://api.rakshacall.org/api/ws/sessions/456", prodUrl)

        // URL with trailing /api
        val apiSuffixUrl = RakshaRealtimeWebSocketClient.buildWebSocketUrl("https://api.rakshacall.org/api", 456L)
        assertEquals("wss://api.rakshacall.org/api/ws/sessions/456", apiSuffixUrl)

        // URL with trailing /api/
        val apiSlashSuffixUrl = RakshaRealtimeWebSocketClient.buildWebSocketUrl("https://api.rakshacall.org/api/", 456L)
        assertEquals("wss://api.rakshacall.org/api/ws/sessions/456", apiSlashSuffixUrl)

        // Native ws:// URL
        val nativeWsUrl = RakshaRealtimeWebSocketClient.buildWebSocketUrl("ws://192.168.1.100:8000", 789L)
        assertEquals("ws://192.168.1.100:8000/api/ws/sessions/789", nativeWsUrl)
    }

    @Test
    fun testHandleDirectRiskUpdateMessage() {
        var receivedUpdate: RealtimeRiskUpdate? = null
        val client = RakshaRealtimeWebSocketClient(
            serverBaseUrl = "http://10.0.2.2:8000",
            sessionId = 1L,
            onRiskUpdate = { update -> receivedUpdate = update }
        )

        val jsonMessage = """
            {
                "type": "risk_update",
                "fused_score": 88.5,
                "risk_level": "CRITICAL",
                "conversation_score": 90.0,
                "visual_score": 75.0,
                "liveness_score": 0.2,
                "reasons": ["Authority Impersonation", "Threat of immediate arrest"],
                "irreversible_action": true,
                "model_mode": "GEMINI_LIVE"
            }
        """.trimIndent()

        client.handleIncomingMessage(jsonMessage)

        assertNotNull("Risk update should be received", receivedUpdate)
        assertEquals(88.5f, receivedUpdate!!.fused_score)
        assertEquals("CRITICAL", receivedUpdate!!.risk_level)
        assertTrue(receivedUpdate!!.irreversible_action)
        assertEquals(2, receivedUpdate!!.reasons.size)
        assertEquals("Authority Impersonation", receivedUpdate!!.reasons[0])
    }

    @Test
    fun testHandleGeminiLiveAiAnalysisEnvelope() {
        var receivedUpdate: RealtimeRiskUpdate? = null
        val client = RakshaRealtimeWebSocketClient(
            serverBaseUrl = "http://10.0.2.2:8000",
            sessionId = 1L,
            onRiskUpdate = { update -> receivedUpdate = update }
        )

        val geminiEnvelope = """
            {
                "type": "ai_analysis",
                "session_id": "1",
                "analysis": {
                    "stage": "ISOLATION",
                    "tactic": "Isolation Tactic"
                },
                "risk_update": {
                    "type": "risk_update",
                    "fused_score": 65.0,
                    "risk_level": "HIGH",
                    "reasons": ["Demanded victim remain in closed room"],
                    "irreversible_action": false
                }
            }
        """.trimIndent()

        client.handleIncomingMessage(geminiEnvelope)

        assertNotNull("Risk update inside ai_analysis should be unpacked", receivedUpdate)
        assertEquals(65.0f, receivedUpdate!!.fused_score)
        assertEquals("HIGH", receivedUpdate!!.risk_level)
        assertFalse(receivedUpdate!!.irreversible_action)
        assertEquals("Demanded victim remain in closed room", receivedUpdate!!.reasons.first())
    }

    @Test
    fun testHandleTranscriptMessage() {
        var receivedTranscript: String? = null
        val client = RakshaRealtimeWebSocketClient(
            serverBaseUrl = "http://10.0.2.2:8000",
            sessionId = 1L,
            onTranscriptReceived = { text -> receivedTranscript = text }
        )

        val jsonTranscript = """
            {
                "type": "transcript",
                "text": "This is Officer Sharma from CBI Mumbai headquarters. Do not disconnect the call.",
                "speaker": "CALLER",
                "confidence": 0.98
            }
        """.trimIndent()

        client.handleIncomingMessage(jsonTranscript)

        assertNotNull("Transcript should be captured", receivedTranscript)
        assertEquals(
            "This is Officer Sharma from CBI Mumbai headquarters. Do not disconnect the call.",
            receivedTranscript
        )
    }

    @Test
    fun testHandleAiStatusMessageUpdatesConnectionState() {
        val client = RakshaRealtimeWebSocketClient(
            serverBaseUrl = "http://10.0.2.2:8000",
            sessionId = 1L
        )

        // Status: unavailable
        client.handleIncomingMessage("""{"type":"ai_status","status":"unavailable"}""")
        assertEquals(AIConnectionState.AI_UNAVAILABLE, client.connectionState.value)

        // Status: connected
        client.handleIncomingMessage("""{"type":"ai_status","status":"connected"}""")
        assertEquals(AIConnectionState.CONNECTED, client.connectionState.value)
    }

    @Test
    fun testHandlePongAndMalformedJsonDoNotCrash() {
        val client = RakshaRealtimeWebSocketClient(
            serverBaseUrl = "http://10.0.2.2:8000",
            sessionId = 1L
        )

        // Application pong
        client.handleIncomingMessage("""{"type":"pong"}""")

        // Completely invalid JSON
        client.handleIncomingMessage("NOT_JSON_AT_ALL{foo")

        // Unknown JSON type
        client.handleIncomingMessage("""{"type":"unknown_future_event","data":123}""")
    }

    @Test
    fun testTerminalSessionErrorIdentification() {
        val client = RakshaRealtimeWebSocketClient(
            serverBaseUrl = "http://10.0.2.2:8000",
            sessionId = 1L
        )

        // 4004 session not found
        assertTrue(client.isTerminalSessionError(4004, "Session not found"))

        // 1008 policy violation
        assertTrue(client.isTerminalSessionError(1008, "Policy violation"))

        // Reason contains invalid
        assertTrue(client.isTerminalSessionError(1000, "Invalid session token"))

        // Transient network failure should NOT be marked terminal (so client can reconnect)
        assertFalse(client.isTerminalSessionError(1006, "Abnormal closure"))
        assertFalse(client.isTerminalSessionError(1001, "Going away"))
    }
}
