package com.raksha.video.webrtc

data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

data class WebRtcConfig(
    val iceServers: List<IceServerConfig> = listOf(IceServerConfig(listOf("stun:stun.l.google.com:19302"))),
    val videoWidth: Int = 1280,
    val videoHeight: Int = 720,
    val videoFps: Int = 24
)
