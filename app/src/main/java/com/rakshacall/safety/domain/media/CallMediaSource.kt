package com.rakshacall.safety.domain.media

import com.rakshacall.safety.domain.model.MediaSourceConnectionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Permitted video-call and media input sources for real-time safety analysis.
 * Strictly complies with platform sandboxing: zero silent interception or unauthorized VoIP decryption.
 */
enum class MediaSourceType(val displayName: String, val category: String) {
    MICROPHONE("Local Microphone", "On-Device Permitted Audio Input"),
    CAMERA("Local Camera", "On-Device CameraX Supporting Visual Input"),
    ANDROID_SCREEN_AUDIO("Android MediaProjection", "User-Consented Platform MediaProjection"),
    GOOGLE_MEET("Google Meet Media API Connector", "Official Cloud Conference Integration"),
    BROWSER_CAPTURE("Browser Capture Extension", "Consented Web Video Call Audio Bridge"),
    MANUAL_TEXT("Manual Text Input", "Direct Permitted Input Fallback"),
    DESKTOP_CAPTURE("Desktop Capture Connector", "Consented Desktop Audio/Screen Bridge")
}

/**
 * Explicit user consent and platform permission status.
 */
enum class MediaConsentStatus(val label: String) {
    GRANTED("Consent Granted"),
    REQUIRED("Permission & Consent Required"),
    REVOKED("Consent Revoked"),
    RESTRICTED_BY_PLATFORM("Platform Restricted (OS Sandboxing)")
}

/**
 * Real-time operational status of an active or standby media source.
 */
data class MediaSourceStatus(
    val sourceType: MediaSourceType,
    val isConnected: Boolean,
    val isAudioAvailable: Boolean,
    val isVideoAvailable: Boolean,
    val isTranscriptAvailable: Boolean,
    val connectionState: MediaSourceConnectionState = if (isConnected) MediaSourceConnectionState.STREAMING else MediaSourceConnectionState.DISCONNECTED,
    val lastSignalTime: Long = 0L,
    val latencyMs: Long = 0L,
    val consentStatus: MediaConsentStatus,
    val detailMessage: String = "",
    val healthQuality: String = if (isConnected) "HEALTHY" else "STANDBY"
)

/**
 * Media chunk emitted by a permitted media source.
 */
data class MediaChunk(
    val id: String,
    val sourceType: MediaSourceType,
    val timestamp: Long = System.currentTimeMillis(),
    val textSnippet: String? = null,
    val audioEnergy: Float? = null,
    val videoQualityScore: Float? = null,
    val speakerLabel: String = "CALLER"
)

/**
 * Abstraction interface for real-world permitted call media sources.
 */
interface CallMediaSource {
    val sourceType: MediaSourceType
    val status: StateFlow<MediaSourceStatus>

    suspend fun connect(): Result<Unit> = startMonitoring()
    suspend fun pause(): Result<Unit> = Result.success(Unit)
    suspend fun resume(): Result<Unit> = Result.success(Unit)
    suspend fun stop(): Result<Unit> = stopMonitoring()

    suspend fun requestConsent(): Boolean
    suspend fun startMonitoring(): Result<Unit>
    suspend fun stopMonitoring(): Result<Unit>
    suspend fun release()
}
