package com.raksha.video.core.security

interface RakshaProtectionBridge {
    suspend fun attachToCall(callId: String)
    suspend fun detachFromCall()
}

class NoOpRakshaProtectionBridge : RakshaProtectionBridge {
    override suspend fun attachToCall(callId: String) = Unit
    override suspend fun detachFromCall() = Unit
}
