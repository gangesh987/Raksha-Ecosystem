package com.rakshacall.safety.webrtc

/**
 * Configuration for real WebRTC media sessions in RakshaCall.
 * Supports configurable STUN/TURN ICE servers with no hardcoded credentials.
 */
data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

data class WebRtcConfig(
    val videoWidth: Int = 1280,
    val videoHeight: Int = 720,
    val videoFps: Int = 30,
    val isVideoEnabled: Boolean = true,
    val isAudioEnabled: Boolean = true,
    val iceServers: List<IceServerConfig> = defaultIceServers()
) {
    companion object {
        fun defaultIceServers(): List<IceServerConfig> = listOf(
            IceServerConfig(urls = listOf("stun:stun.l.google.com:19302")),
            IceServerConfig(urls = listOf("stun:stun1.l.google.com:19302")),
            IceServerConfig(urls = listOf("stun:stun2.l.google.com:19302"))
        )
    }
}
