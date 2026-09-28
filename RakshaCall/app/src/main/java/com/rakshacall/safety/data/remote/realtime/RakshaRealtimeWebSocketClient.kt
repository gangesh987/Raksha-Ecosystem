package com.rakshacall.safety.data.remote.realtime

import com.rakshacall.safety.core.util.RakshaLogger
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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var reconnectJob: Job? = null
    private val shouldReconnect = AtomicBoolean(false)
    private var reconnectAttempt = 0

    companion object {
        fun buildWebSocketUrl(serverBaseUrl: String, sessionId: Long): String {
            val cleanUrl = serverBaseUrl.trim()
                .removeSuffix("/")
                .removeSuffix("/api")
                .replace("http://", "ws://")
                .replace("https://", "wss://")
            val scheme = if (cleanUrl.startsWith("ws://") || cleanUrl.startsWith("wss://")) "" else "ws://"
            return "$scheme$cleanUrl/api/ws/sessions/$sessionId"
        }
    }

    fun connect() {
        shouldReconnect.set(true)
        initiateConnection()
    }

    private fun initiateConnection() {
        if (serverBaseUrl.isBlank() || sessionId <= 0) {
            _connectionState.value = AIConnectionState.AI_UNAVAILABLE
            return
        }

        _connectionState.value = if (reconnectAttempt > 0) AIConnectionState.RECONNECTING else AIConnectionState.CONNECTING

        val wsUrl = buildWebSocketUrl(serverBaseUrl, sessionId)
        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                _connectionState.value = AIConnectionState.CONNECTED
                RakshaLogger.e2e("T1_WS_CONNECTED", "ts=${System.currentTimeMillis()} session=$sessionId url=$serverBaseUrl")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Binary audio / frame payload handled if sent by server
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = AIConnectionState.OFFLINE
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = AIConnectionState.OFFLINE
                if (isTerminalSessionError(code, reason)) {
                    shouldReconnect.set(false)
                } else {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = AIConnectionState.OFFLINE
                val httpCode = response?.code ?: 0
                if (httpCode in listOf(400, 401, 403, 404)) {
                    // Invalid session or unauthorized — do not reconnect in tight loop
                    shouldReconnect.set(false)
                } else {
                    scheduleReconnect()
                }
            }
        })
    }

    internal fun handleIncomingMessage(rawText: String) {
        try {
            val jsonElement = json.parseToJsonElement(rawText)
            val jsonObject = jsonElement.jsonObject
            val type = jsonObject["type"]?.jsonPrimitive?.content ?: ""

            when (type) {
                "risk_update" -> {
                    val update = json.decodeFromJsonElement<RealtimeRiskUpdate>(jsonElement)
                    RakshaLogger.e2e("T4_RISK_RECEIVED", "ts=${System.currentTimeMillis()} session=$sessionId risk=${update.risk_level} score=${update.fused_score}")
                    onRiskUpdate(update)
                    RakshaLogger.e2e("T5_UI_UPDATED", "ts=${System.currentTimeMillis()} session=$sessionId risk=${update.risk_level}")
                }
                "ai_analysis" -> {
                    val riskUpdateElem = jsonObject["risk_update"]
                    if (riskUpdateElem != null) {
                        val update = json.decodeFromJsonElement<RealtimeRiskUpdate>(riskUpdateElem)
                        onRiskUpdate(update)
                    }
                }
                "transcript" -> {
                    val text = jsonObject["text"]?.jsonPrimitive?.content ?: ""
                    if (text.isNotBlank()) {
                        onTranscriptReceived(text)
                    }
                }
                "ai_status" -> {
                    val status = jsonObject["status"]?.jsonPrimitive?.content ?: ""
                    if (status.equals("unavailable", ignoreCase = true)) {
                        _connectionState.value = AIConnectionState.AI_UNAVAILABLE
                    } else if (status.equals("connected", ignoreCase = true)) {
                        _connectionState.value = AIConnectionState.CONNECTED
                    }
                }
                "pong" -> {
                    // Application-level pong acknowledged
                }
            }
        } catch (_: Exception) {
            // Safe JSON parse error suppression
        }
    }

    internal fun isTerminalSessionError(code: Int, reason: String): Boolean {
        return code in 4000..4099 ||
                code == 1008 ||
                reason.contains("invalid", ignoreCase = true) ||
                reason.contains("not found", ignoreCase = true)
    }

    fun sendTranscript(text: String) {
        if (text.isBlank() || _connectionState.value != AIConnectionState.CONNECTED) return
        val escaped = json.encodeToString(String.serializer(), text)
        RakshaLogger.e2e("T2_INPUT_SENT", "ts=${System.currentTimeMillis()} session=$sessionId len=${text.length}")
        webSocket?.send("{\"type\":\"text\",\"text\":$escaped}")
    }

    fun sendPcm16AudioBase64(base64Data: String) {
        if (base64Data.isBlank() || _connectionState.value != AIConnectionState.CONNECTED) return
        webSocket?.send("{\"type\":\"audio_pcm16\",\"data\":\"$base64Data\"}")
    }

    fun sendAudioBytes(bytes: ByteArray) {
        if (bytes.isEmpty() || _connectionState.value != AIConnectionState.CONNECTED) return
        webSocket?.send(ByteString.of(*bytes))
    }

    fun sendPing() {
        if (_connectionState.value == AIConnectionState.CONNECTED) {
            webSocket?.send("{\"type\":\"ping\"}")
        }
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
