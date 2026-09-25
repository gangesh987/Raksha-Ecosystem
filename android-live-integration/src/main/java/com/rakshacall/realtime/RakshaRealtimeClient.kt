package com.rakshacall.realtime

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.ByteString

@Serializable
data class LiveMessage(val type: String, val text: String? = null, val data: String? = null)

class RakshaRealtimeClient(
    private val baseUrl: String,
    private val sessionId: Long,
    private val onMessage: (String) -> Unit,
    private val onState: (Boolean) -> Unit
) {
    private val client = OkHttpClient()
    private var socket: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun connect() {
        val url = baseUrl.trimEnd('/') + "/api/ws/sessions/$sessionId"
        socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { onState(true) }
            override fun onMessage(webSocket: WebSocket, text: String) { onMessage(text) }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { onState(false) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { onState(false) }
        })
    }

    fun sendTranscript(text: String) {
        if (text.isBlank()) return
        socket?.send("{\"type\":\"text\",\"text\":${Json.encodeToString(text)} }")
    }

    fun sendPcm16(base64: String) {
        socket?.send("{\"type\":\"audio_pcm16\",\"data\":\"$base64\"}")
    }

    fun close() { socket?.close(1000, "user stopped protection"); socket = null; client.dispatcher.executorService.shutdown() }
}
