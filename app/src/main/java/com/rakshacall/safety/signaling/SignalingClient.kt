package com.rakshacall.safety.signaling

import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SignalingClient(
    private val serverUrl: String,
    private val token: String,
    private val listener: Listener
) {
    interface Listener {
        fun onOpen()
        fun onClosed()
        fun onFailure(message: String)
        fun onMessage(type: String, payload: JSONObject)
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val processedRequestIds = ConcurrentHashMap.newKeySet<String>()

    fun connect() {
        require(serverUrl.startsWith("ws://") || serverUrl.startsWith("wss://")) {
            "Invalid signaling URL: $serverUrl"
        }
        val request = Request.Builder()
            .url(serverUrl)
            .apply {
                if (token.isNotBlank()) {
                    header("Authorization", if (token.startsWith("Bearer ")) token else "Bearer $token")
                }
            }
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                listener.onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    val reqId = json.optString("request_id")
                    if (reqId.isNotBlank() && !processedRequestIds.add(reqId)) {
                        // Duplicate message already processed
                        return
                    }
                    val type = json.optString("type")
                    val payload = json.optJSONObject("payload") ?: JSONObject()
                    listener.onMessage(type, payload)
                }.onFailure {
                    listener.onFailure("Malformed signaling JSON: ${it.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                webSocket.close(1000, null)
                listener.onClosed()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                listener.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
                listener.onFailure(t.message ?: "Signaling socket failure")
            }
        })
    }

    fun send(type: String, payload: JSONObject = JSONObject()) {
        val reqId = UUID.randomUUID().toString()
        val envelope = JSONObject().apply {
            put("version", 1)
            put("type", type)
            put("request_id", reqId)
            put("timestamp", System.currentTimeMillis())
            put("payload", payload)
        }
        socket?.send(envelope.toString())
    }

    fun sendCallInvite(callId: String, callerName: String) {
        send("call_invite", JSONObject().apply {
            put("call_id", callId)
            put("caller_name", callerName)
        })
    }

    fun sendCallAccept(callId: String) {
        send("call_accept", JSONObject().apply {
            put("call_id", callId)
        })
    }

    fun sendCallReject(callId: String, reason: String = "busy") {
        send("call_reject", JSONObject().apply {
            put("call_id", callId)
            put("reason", reason)
        })
    }

    fun sendCallEnd(callId: String) {
        send("call_end", JSONObject().apply {
            put("call_id", callId)
        })
    }

    fun sendSdpOffer(callId: String, sdp: String) {
        send("sdp_offer", JSONObject().apply {
            put("call_id", callId)
            put("sdp", sdp)
        })
    }

    fun sendSdpAnswer(callId: String, sdp: String) {
        send("sdp_answer", JSONObject().apply {
            put("call_id", callId)
            put("sdp", sdp)
        })
    }

    fun sendIceCandidate(callId: String, sdpMid: String?, sdpMLineIndex: Int, candidate: String) {
        send("ice_candidate", JSONObject().apply {
            put("call_id", callId)
            put("sdp_mid", sdpMid)
            put("sdp_mline_index", sdpMLineIndex)
            put("candidate", candidate)
        })
    }

    fun close() {
        isConnected.set(false)
        runCatching { socket?.close(1000, "client_closed") }
        socket = null
        client.dispatcher.executorService.shutdown()
        processedRequestIds.clear()
    }
}
