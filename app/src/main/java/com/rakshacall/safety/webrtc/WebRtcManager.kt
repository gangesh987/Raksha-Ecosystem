package com.rakshacall.safety.webrtc

import kotlinx.coroutines.flow.StateFlow
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

sealed class WebRtcState {
    object New : WebRtcState()
    object Connecting : WebRtcState()
    object Connected : WebRtcState()
    object Disconnected : WebRtcState()
    object Failed : WebRtcState()
    object Closed : WebRtcState()
}

interface WebRtcManager {
    val state: StateFlow<WebRtcState>
    fun initialize()
    fun createOffer(onCreated: (SessionDescription) -> Unit)
    fun createAnswer(onCreated: (SessionDescription) -> Unit)
    fun setRemoteDescription(description: SessionDescription, onDone: () -> Unit)
    fun addIceCandidate(candidate: IceCandidate)
    fun enableCamera()
    fun disableCamera()
    fun muteMicrophone()
    fun unmuteMicrophone()
    fun switchCamera()
    fun closePeerConnection()
    fun release()
    fun localVideoTrack(): VideoTrack?
    fun isCameraEnabled(): Boolean
    fun isMicEnabled(): Boolean
}
