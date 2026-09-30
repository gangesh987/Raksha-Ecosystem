package com.rakshacall.safety.data.remote.config

import com.example.rakshacall.BuildConfig

/**
 * Dynamic configuration management.
 * Avoids hardcoded developer IPs, credentials, or private tunnels.
 * Supports configurable development and production environments.
 */
object AppConfig {

    const val APP_VERSION = "2.1.0-VALIDATION"
    val PRODUCTION_BASE_URL = BuildConfig.DEFAULT_BACKEND_URL
    val DEFAULT_BASE_URL = if (BuildConfig.DEBUG) "http://10.0.2.2:8000" else PRODUCTION_BASE_URL

    const val PUBLIC_CLOUD_BASE_URL = "https://api.rakshacall.org"
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8000"
    const val PC_LAN_BASE_URL = "http://192.168.1.100:8000"

    enum class EnvironmentPreset(val title: String, val defaultUrl: String, val description: String) {
        PRODUCTION("Production (Cloud)", "https://api.rakshacall.org", "Production cloud API endpoint"),
        EMULATOR("Android Emulator", "http://10.0.2.2:8000", "Default host loopback for Android emulator"),
        CUSTOM_LAN("Custom PC Wi-Fi LAN", "", "Enter your PC's IP address (e.g. http://192.168.1.X:8000)"),
        DEV_TUNNEL("Secure Dev Tunnel", "", "Custom HTTPS/WSS development tunnel (e.g. Cloudflare / ngrok)")
    }

    @Volatile
    var activeBaseUrl: String = DEFAULT_BASE_URL
        set(value) {
            field = normalizeUrl(value)
        }

    fun normalizeUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.isEmpty()) return DEFAULT_BASE_URL
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        return if (trimmed.endsWith("/")) trimmed.dropLast(1) else trimmed
    }

    var isCloudSyncEnabled: Boolean = false
    var isRemoteAiEnabled: Boolean = true
    var isFcmEnabled: Boolean = false
    var isMultilingualEnabled: Boolean = true

    // Feature flags
    object FeatureFlags {
        const val ENABLE_CLOUD_SYNC = "enable_cloud_sync"
        const val ENABLE_REMOTE_AI = "enable_remote_ai"
        const val ENABLE_VISUAL_ANALYSIS = "enable_visual_analysis"
        const val ENABLE_LIVENESS = "enable_liveness"
        const val ENABLE_MULTILINGUAL = "enable_multilingual"
        const val ENABLE_FCM = "enable_fcm"
        const val ENABLE_ADVANCED_EVIDENCE = "enable_advanced_evidence"
    }
}
