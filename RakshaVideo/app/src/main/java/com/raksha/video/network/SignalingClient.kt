package com.raksha.video.network

import okhttp3.*
import org.json.JSONObject
import java.util.UUID

class SignalingClient(
    private val serverUrl: String,
    private val token: String,
    private val listener: Listener
) {
    interface Listener {
        fun onOpen()
        fun onClosed()
        fun onFailure(message: String)
        fun onMessage(message: JSONObject)
    }
    private val client = OkHttpClient()
    private var socket: WebSocket? = null

    fun connect() {
        require(serverUrl.startsWith("wss://") || serverUrl.startsWith("ws://")) { "Invalid signaling URL" }
        require(token.isNotBlank()) { "API token is not configured" }
        val request = Request.Builder().url(serverUrl).header("Authorization", "Bearer $token").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = listener.onOpen()
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching { listener.onMessage(JSONObject(text)) }.onFailure { listener.onFailure("Invalid signaling message") }
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(1000, null); listener.onClosed() }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = listener.onClosed()
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = listener.onFailure(t.message ?: "WebSocket failure")
        })
    }

    fun send(type: String, payload: JSONObject = JSONObject()) {
        val envelope = JSONObject().apply {
            put("version", 1)
            put("type", type)
            put("request_id", UUID.randomUUID().toString())
            put("timestamp", System.currentTimeMillis())
            put("payload", payload)
        }
        socket?.send(envelope.toString())
    }

    fun close() { socket?.close(1000, "client_close"); socket = null; client.dispatcher.executorService.shutdown() }
}
