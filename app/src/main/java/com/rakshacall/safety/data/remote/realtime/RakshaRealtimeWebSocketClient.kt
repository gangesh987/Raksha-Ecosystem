package com.rakshacall.safety.data.remote.realtime

import com.rakshacall.safety.domain.provider.AIConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Serializable
data class RealtimeRiskUpdate(
    val type: String = "risk_update",
    val fused_score: Float? = null,
    val risk_level: String? = null,
    val conversation_score: Float? = null,
    val visual_score: Float? = null,
    val liveness_score: Float? = null,
    val reasons: List<String> = emptyList(),
    val irreversible_action: Boolean = false,
    val model_mode: String? = null
)

class RakshaRealtimeWebSocketClient(
    private val serverBaseUrl: String,
    private val sessionId: Long,
    private val onRiskUpdate: (RealtimeRiskUpdate) -> Unit = {},
    private val onTranscriptReceived: (String) -> Unit = {}
) {
    private val _connectionState = MutableStateFlow(AIConnectionState.OFFLINE)
    val connectionState: StateFlow<AIConnectionState> = _connectionState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var reconnectJob: Job? = null
    private val shouldReconnect = AtomicBoolean(false)
    private var reconnectAttempt = 0

    fun connect() {
        shouldReconnect.set(true)
        initiateConnection()
    }

    private fun initiateConnection() {
        if (serverBaseUrl.isBlank()) {
            _connectionState.value = AIConnectionState.AI_UNAVAILABLE
            return
        }

        _connectionState.value = if (reconnectAttempt > 0) AIConnectionState.RECONNECTING else AIConnectionState.CONNECTING

        val wsUrl = serverBaseUrl.trimEnd('/')
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/api/ws/sessions/$sessionId"

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                _connectionState.value = AIConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Audio / binary frames
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = AIConnectionState.OFFLINE
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = AIConnectionState.OFFLINE
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = AIConnectionState.OFFLINE
                scheduleReconnect()
            }
        })
    }

    private fun handleIncomingMessage(rawText: String) {
        try {
            if (rawText.contains("\"type\":\"risk_update\"")) {
                val update = json.decodeFromString<RealtimeRiskUpdate>(rawText)
                onRiskUpdate(update)
            } else if (rawText.contains("\"type\":\"transcript\"")) {
                val elem = json.parseToJsonElement(rawText)
                val text = elem.toString()
                onTranscriptReceived(text)
            } else if (rawText.contains("\"type\":\"ai_status\"") && rawText.contains("\"unavailable\"")) {
                _connectionState.value = AIConnectionState.AI_UNAVAILABLE
            }
        } catch (_: Exception) {
            // Safe JSON parse error suppression
        }
    }

    fun sendTranscript(text: String) {
        if (text.isBlank() || _connectionState.value != AIConnectionState.CONNECTED) return
        val escaped = json.encodeToString(String.serializer(), text)
        webSocket?.send("{\"type\":\"text\",\"text\":$escaped}")
    }


    fun sendPcm16AudioBase64(base64Data: String) {
        if (base64Data.isBlank() || _connectionState.value != AIConnectionState.CONNECTED) return
        webSocket?.send("{\"type\":\"audio_pcm16\",\"data\":\"$base64Data\"}")
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect.get()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempt++
            val backoffMs = (1000L * (1 shl (reconnectAttempt.coerceAtMost(5)))).coerceAtMost(30000L)
            delay(backoffMs)
            if (shouldReconnect.get()) {
                initiateConnection()
            }
        }
    }

    fun disconnect() {
        shouldReconnect.set(false)
        reconnectJob?.cancel()
        webSocket?.close(1000, "Normal closure by user")
        webSocket = null
        _connectionState.value = AIConnectionState.OFFLINE
    }
}
