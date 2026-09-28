package com.raksha.video.core.security

import com.raksha.video.BuildConfig

object ProductionConfig {
    val backendHttpUrl: String = BuildConfig.BACKEND_HTTP_URL.trimEnd('/')
    val signalingWsUrl: String = BuildConfig.SIGNALING_WS_URL
    val apiToken: String = BuildConfig.API_TOKEN

    fun validate(): Result<Unit> {
        if (backendHttpUrl.isBlank() || signalingWsUrl.isBlank()) {
            return Result.failure(IllegalStateException("Raksha backend endpoints are not configured"))
        }
        if (!BuildConfig.DEBUG) {
            if (!backendHttpUrl.startsWith("https://")) {
                return Result.failure(IllegalStateException("Production backend must use HTTPS"))
            }
            if (!signalingWsUrl.startsWith("wss://")) {
                return Result.failure(IllegalStateException("Production signaling must use WSS"))
            }
        }
        if (apiToken.isBlank()) return Result.failure(IllegalStateException("Raksha API token is not configured"))
        return Result.success(Unit)
    }
}
