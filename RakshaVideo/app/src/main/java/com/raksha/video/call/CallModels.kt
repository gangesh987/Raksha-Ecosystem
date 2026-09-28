package com.raksha.video.call

sealed interface CallState {
    data object Idle : CallState
    data object Creating : CallState
    data class Created(val callId: String) : CallState
    data class Joining(val callId: String) : CallState
    data class Waiting(val callId: String) : CallState
    data object Negotiating : CallState
    data object Connecting : CallState
    data class Connected(val callId: String) : CallState
    data object Reconnecting : CallState
    data object Ending : CallState
    data object Ended : CallState
    data class Failed(val message: String) : CallState
}

data class CallParticipant(val id: String, val displayName: String)

enum class ConnectionQuality { EXCELLENT, GOOD, FAIR, POOR, DISCONNECTED }
