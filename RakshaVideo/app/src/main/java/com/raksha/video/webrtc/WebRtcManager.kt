package com.raksha.video.webrtc

import kotlinx.coroutines.flow.StateFlow
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface WebRtcManager {
    val state: StateFlow<WebRtcState>
    fun initialize()
    fun createOffer(onCreated: (SessionDescription) -> Unit)
    fun createAnswer(onCreated: (SessionDescription) -> Unit)
    fun setRemoteDescription(description: SessionDescription, onDone: () -> Unit = {})
    fun addIceCandidate(candidate: IceCandidate)
    fun enableCamera()
    fun disableCamera()
    fun muteMicrophone()
    fun unmuteMicrophone()
    fun switchCamera()
    fun closePeerConnection()
    fun release()
}

sealed interface WebRtcState {
    data object New : WebRtcState
    data object Connecting : WebRtcState
    data object Connected : WebRtcState
    data object Disconnected : WebRtcState
    data object Failed : WebRtcState
    data object Closed : WebRtcState
}
