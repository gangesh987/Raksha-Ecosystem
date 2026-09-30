package com.rakshacall.safety.webrtc

/**
 * Real WebRTC Call Lifecycle States (Phase 2):
 * OUTGOING -> RINGING -> CONNECTING -> CONNECTED -> RECONNECTING -> DISCONNECTED -> ENDED.
 *
 * CONNECTED is not a UI-only state: it requires actual PeerConnection established
 * and actual media tracks available.
 */
enum class CallLifecycleState(val displayName: String) {
    OUTGOING("Outgoing"),
    RINGING("Ringing"),
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    RECONNECTING("Reconnecting"),
    DISCONNECTED("Disconnected"),
    ENDED("Ended");

    val isTerminal: Boolean
        get() = this == ENDED || this == DISCONNECTED

    val isMediaActive: Boolean
        get() = this == CONNECTED
}
