package com.raksha.video.protection

import com.raksha.video.core.security.ProductionConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RakshaProtectionClient(private val apiBaseUrl: String = ProductionConfig.backendHttpUrl, private val token: String = ProductionConfig.apiToken, private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) : RakshaProtectionBridge {
    private val client = OkHttpClient()
    private val _state = MutableStateFlow(ProtectionState.DISABLED)
    private val _risk = MutableSharedFlow<RiskEvent>(extraBufferCapacity = 32)
    private val _timeline = MutableStateFlow<List<ProtectionTimelineEvent>>(emptyList())
    private var session: ProtectionSession? = null
    private var socket: WebSocket? = null
    private var visualAnalysisEnabled = false
    private var conversationAnalysisEnabled = false
    override fun observeState(): StateFlow<ProtectionState> = _state.asStateFlow()
    override fun observeRiskEvents(): Flow<RiskEvent> = _risk.asSharedFlow()
    override fun observeTimeline(): Flow<List<ProtectionTimelineEvent>> = _timeline.asStateFlow()

    override suspend fun attachToCall(callId: String, consent: ConsentState): Result<ProtectionSession> {
        ProductionConfig.validate().getOrElse { _state.value = ProtectionState.ERROR; return Result.failure(it) }
        if (!consent.riskDetection && !consent.conversationAnalysis && !consent.visualAnalysis) { _state.value = ProtectionState.DISABLED; return Result.failure(IllegalStateException("Protection consent not enabled")) }
        _state.value = ProtectionState.CONNECTING
        visualAnalysisEnabled = consent.visualAnalysis
        conversationAnalysisEnabled = consent.conversationAnalysis
        val body = JSONObject().apply { put("call_id", callId); put("media", JSONObject().put("audio", true).put("video", true)); put("consent", JSONObject().apply { put("audio_analysis", consent.conversationAnalysis); put("visual_analysis", consent.visualAnalysis); put("risk_detection", consent.riskDetection); put("trusted_contact_alerts", consent.trustedContactAlerts) }) }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$apiBaseUrl/api/protection/sessions").header("Authorization", "Bearer $token").post(body).build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Protection session HTTP ${response.code}")
                val json = JSONObject(response.body?.string() ?: error("Empty protection response"))
                val rawWs = json.getString("websocket_url")
                val wsUrl = if (rawWs.contains("127.0.0.1")) rawWs.replace("127.0.0.1", apiBaseUrl.substringAfter("://").substringBefore(":")) else rawWs
                val created = ProtectionSession(json.getString("session_id"), json.getString("call_id"), wsUrl, json.optString("expires_at", null))
                session = created; openSocket(created); addTimeline("Protection session created"); created
            }
        }.onFailure { _state.value = ProtectionState.ERROR; addTimeline("Protection unavailable", it.message) }
    }
    private fun openSocket(s: ProtectionSession) {
        val request = Request.Builder().url(s.websocketUrl).header("Authorization", "Bearer $token").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { _state.value = ProtectionState.MONITORING; addTimeline("Protection connected"); webSocket.send(envelope("protection_started", s.callId, s.sessionId).toString()) }
            override fun onMessage(webSocket: WebSocket, text: String) { handleMessage(text) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { if (_state.value != ProtectionState.DISCONNECTING && _state.value != ProtectionState.DISABLED) _state.value = ProtectionState.DISCONNECTED }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { if (_state.value != ProtectionState.DISCONNECTING) { _state.value = ProtectionState.ERROR; addTimeline("Protection service disconnected", t.message) } }
        })
    }
    private fun handleMessage(text: String) { runCatching { val root = JSONObject(text); when (root.optString("type")) {
        "risk_update" -> { val p = root.optJSONObject("payload") ?: JSONObject(); val level = runCatching { RiskLevel.valueOf(p.optString("level", "UNKNOWN").uppercase()) }.getOrDefault(RiskLevel.UNKNOWN); val reasons = buildList { val a = p.optJSONArray("reasons") ?: JSONArray(); for (i in 0 until a.length()) add(a.optString(i)) }; val signals = buildList { val a = p.optJSONArray("signals") ?: JSONArray(); for (i in 0 until a.length()) add(a.optString(i)) }; val event = RiskEvent(root.optString("event_id", UUID.randomUUID().toString()), root.optString("session_id", session?.sessionId.orEmpty()), level, if (p.has("score") && !p.isNull("score")) p.optInt("score") else null, reasons, root.optLong("timestamp", System.currentTimeMillis()), if (p.has("confidence")) p.optDouble("confidence").toFloat() else null, signals, p.optString("stage", null)); _state.value = ProtectionState.RISK_DETECTED; _risk.tryEmit(event); addTimeline("Risk update received", "${event.level}${event.score?.let { " ($it)" } ?: ""}"); _state.value = ProtectionState.MONITORING }
        "safety_action" -> { _state.value = ProtectionState.INTERVENTION; addTimeline("Safety action received", root.optJSONObject("payload")?.optString("action")) }
    } } }
    private fun envelope(type: String, callId: String, sessionId: String, payload: JSONObject = JSONObject()) = JSONObject().apply { put("version", 1); put("type", type); put("event_id", UUID.randomUUID().toString()); put("session_id", sessionId); put("call_id", callId); put("timestamp", System.currentTimeMillis()); put("payload", payload) }
    override suspend fun sendEvent(event: ProtectionEvent) { val s = session ?: return; val payload = JSONObject(); event.payload.forEach { (k, v) -> payload.put(k, v) }; socket?.send(envelope(event.type, event.callId, s.sessionId, payload).toString()) }
    suspend fun submitTranscript(text: String, speaker: String = "UNKNOWN", confidence: Float? = null) {
        if (!conversationAnalysisEnabled) return
        val s = session ?: return
        val payload = JSONObject().apply { put("text", text); put("speaker", speaker); if (confidence != null) put("confidence", confidence); put("timestamp", System.currentTimeMillis()); put("source", "client") }
        postJson("$apiBaseUrl/api/protection/sessions/${s.sessionId}/transcript", payload)
    }

    suspend fun submitVisualSignal(signalType: String, confidence: Float? = null, metadata: Map<String, Any?> = emptyMap()) {
        if (!visualAnalysisEnabled) return
        val s = session ?: return
        val payload = JSONObject().apply { put("signal_type", signalType); if (confidence != null) put("confidence", confidence); put("timestamp", System.currentTimeMillis()); put("source", "video_frame_sampler"); metadata.forEach { (k, v) -> put(k, v) } }
        postJson("$apiBaseUrl/api/protection/sessions/${s.sessionId}/visual", payload)
    }

    private fun postJson(url: String, payload: JSONObject) {
        runCatching {
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").post(payload.toString().toRequestBody("application/json".toMediaType())).build()
            client.newCall(req).execute().use { if (!it.isSuccessful) addTimeline("Intelligence request failed", "HTTP ${it.code}") }
        }.onFailure { addTimeline("Intelligence unavailable", it.message) }
    }

    override suspend fun detachFromCall() { val s = session ?: return; _state.value = ProtectionState.DISCONNECTING; sendEvent(ProtectionEvent("protection_stopped", s.callId, s.sessionId)); socket?.close(1000, "call ended"); socket = null; runCatching { val req = Request.Builder().url("$apiBaseUrl/api/protection/sessions/${s.sessionId}/end").header("Authorization", "Bearer $token").post("{}".toRequestBody("application/json".toMediaType())).build(); client.newCall(req).execute().close() }; session = null; _state.value = ProtectionState.DISABLED; addTimeline("Protection session ended") }
    fun close() { socket?.close(1000, "client cleared"); socket = null; client.dispatcher.executorService.shutdown() }
    private fun addTimeline(title: String, detail: String? = null) { _timeline.update { (it + ProtectionTimelineEvent(System.currentTimeMillis(), title, detail)).takeLast(100) } }
}
