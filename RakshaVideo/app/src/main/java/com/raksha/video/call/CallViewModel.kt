package com.raksha.video.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raksha.video.network.SignalingClient
import com.raksha.video.core.security.ProductionConfig
import com.raksha.video.webrtc.IceServerConfig
import com.raksha.video.webrtc.WebRtcConfig
import com.raksha.video.webrtc.WebRtcEngine
import com.raksha.video.webrtc.WebRtcState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class CallViewModel(app: Application) : AndroidViewModel(app) {
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    private val _remoteVideo = MutableStateFlow<org.webrtc.VideoTrack?>(null)
    val remoteVideo: StateFlow<org.webrtc.VideoTrack?> = _remoteVideo.asStateFlow()
    private val _localVideo = MutableStateFlow<org.webrtc.VideoTrack?>(null)
    val localVideo: StateFlow<org.webrtc.VideoTrack?> = _localVideo.asStateFlow()
    private val _micEnabled = MutableStateFlow(true)
    val micEnabled: StateFlow<Boolean> = _micEnabled.asStateFlow()
    private val _cameraEnabled = MutableStateFlow(true)
    val cameraEnabled: StateFlow<Boolean> = _cameraEnabled.asStateFlow()
    private val _connectionLabel = MutableStateFlow("Connecting")
    val connectionLabel: StateFlow<String> = _connectionLabel.asStateFlow()
    private var signaling: SignalingClient? = null
    private var engine: WebRtcEngine? = null
    private var currentCallId: String? = null
    private var isCaller = false


    fun createCall() {
        _callState.value = CallState.Creating
        currentCallId = generateCallId()
        _callState.value = CallState.Created(currentCallId!!)
    }

    fun join(callId: String, caller: Boolean) {
        if (callId.isBlank()) return
        currentCallId = callId.trim().uppercase()
        isCaller = caller
        _callState.value = if (caller) CallState.Joining(currentCallId!!) else CallState.Joining(currentCallId!!)
        val configResult = ProductionConfig.validate()
        if (configResult.isFailure) { _callState.value = CallState.Failed(configResult.exceptionOrNull()?.message ?: "Backend configuration missing"); return }
        setupWebRtc()
        signaling = SignalingClient(ProductionConfig.signalingWsUrl, ProductionConfig.apiToken, object : SignalingClient.Listener {
            override fun onOpen() {
                signaling?.send("join_call", JSONObject().apply { put("call_id", currentCallId); put("display_name", "Raksha User") })
                if (isCaller) _callState.value = CallState.Waiting(currentCallId!!)
            }
            override fun onClosed() {
                if (_callState.value is CallState.Connected) _callState.value = CallState.Reconnecting
            }
            override fun onFailure(message: String) { _callState.value = CallState.Failed(message) }
            override fun onMessage(message: JSONObject) { handleSignal(message) }
        })
        signaling!!.connect()
    }

    private fun setupWebRtc() {
        engine = WebRtcEngine(getApplication(), WebRtcConfig(iceServers = listOf(IceServerConfig(listOf("stun:stun.l.google.com:19302")))),
            onLocalIceCandidate = { c ->
                signaling?.send("ice_candidate", JSONObject().apply {
                    put("call_id", currentCallId); put("sdpMid", c.sdpMid); put("sdpMLineIndex", c.sdpMLineIndex); put("candidate", c.sdp)
                })
            },
            onRemoteVideo = { track -> track.setEnabled(true); _remoteVideo.value = track },
            onConnectionState = { state ->
                when (state) {
                    WebRtcState.Connected -> { _connectionLabel.value = "Connected"; currentCallId?.let { _callState.value = CallState.Connected(it) } }
                    WebRtcState.Connecting -> { _connectionLabel.value = "Connecting"; _callState.value = CallState.Connecting }
                    WebRtcState.Disconnected -> { _connectionLabel.value = "Reconnecting…"; _callState.value = CallState.Reconnecting }
                    WebRtcState.Failed -> { _connectionLabel.value = "Connection failed"; _callState.value = CallState.Failed("WebRTC ICE connection failed") }
                    WebRtcState.Closed -> _connectionLabel.value = "Ended"
                    else -> Unit
                }
            })
        engine!!.initialize()
        _localVideo.value = engine!!.localVideoTrack()
    }

    private fun handleSignal(message: JSONObject) {
        val type = message.optString("type")
        val payload = message.optJSONObject("payload") ?: JSONObject()
        when (type) {
            "participant_joined" -> if (isCaller) {
                _callState.value = CallState.Negotiating
                engine?.createOffer { desc -> sendSdp("offer", desc) }
            }
            "call_joined" -> if (!isCaller) {
                _callState.value = CallState.Waiting(currentCallId!!)
            }
            "offer" -> {
                _callState.value = CallState.Negotiating
                val desc = SessionDescription(SessionDescription.Type.OFFER, payload.getString("sdp"))
                engine?.setRemoteDescription(desc) { engine?.createAnswer { answer -> sendSdp("answer", answer) } }
            }
            "answer" -> {
                val desc = SessionDescription(SessionDescription.Type.ANSWER, payload.getString("sdp"))
                engine?.setRemoteDescription(desc)
            }
            "ice_candidate" -> {
                val candidate = IceCandidate(payload.optString("sdpMid"), payload.optInt("sdpMLineIndex"), payload.optString("candidate"))
                engine?.addIceCandidate(candidate)
            }
            "participant_left", "call_ended" -> endLocal()
        }
    }

    private fun sendSdp(type: String, description: SessionDescription) {
        signaling?.send(type, JSONObject().apply { put("call_id", currentCallId); put("type", description.type.canonicalForm()); put("sdp", description.description) })
    }

    fun toggleMic() {
        val enabled = !_micEnabled.value
        _micEnabled.value = enabled
        if (enabled) engine?.unmuteMicrophone() else engine?.muteMicrophone()
        signaling?.send("mute_changed", JSONObject().apply { put("call_id", currentCallId); put("enabled", enabled) })
    }

    fun toggleCamera() {
        val enabled = !_cameraEnabled.value
        _cameraEnabled.value = enabled
        if (enabled) engine?.enableCamera() else engine?.disableCamera()
        signaling?.send("camera_changed", JSONObject().apply { put("call_id", currentCallId); put("enabled", enabled) })
    }

    fun switchCamera() { engine?.switchCamera() }

    fun endCall() {
        signaling?.send("call_ended", JSONObject().apply { put("call_id", currentCallId) })
        endLocal()
    }

    private fun endLocal() {
        _callState.value = CallState.Ending
        signaling?.close(); signaling = null
        engine?.release(); engine = null
        _remoteVideo.value = null
        _localVideo.value = null
        _callState.value = CallState.Ended
    }

    private fun generateCallId(): String = "RCV-" + (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")

    override fun onCleared() { endLocal() }
}
