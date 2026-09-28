package com.rakshacall.safety.data.remote.config

/**
 * Dynamic configuration management.
 * Avoids hardcoded production URLs and supports environment switching.
 */
object AppConfig {

    const val APP_VERSION = "2.0.0-PROD"
    const val PUBLIC_CLOUD_BASE_URL = "https://poor-keys-like.loca.lt"
    const val PC_LAN_BASE_URL = "http://172.17.35.95:8000"
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8000"
    const val DEFAULT_BASE_URL = PUBLIC_CLOUD_BASE_URL

    enum class EnvironmentPreset(val title: String, val url: String, val description: String) {
        PUBLIC_CLOUD("Public Cloud (Live)", PUBLIC_CLOUD_BASE_URL, "Live public HTTPS/WSS backend outside LAN"),
        PC_LAN("PC Development Host", PC_LAN_BASE_URL, "Direct Wi-Fi connection to PC (172.17.35.95:8000)"),
        EMULATOR("Android Emulator", EMULATOR_BASE_URL, "Default host loopback (10.0.2.2:8000)"),
        PRODUCTION("Production (Cloud)", "https://api.rakshacall.org", "Production cloud API endpoint"),
        CUSTOM("Custom LAN / Server", "", "Custom physical device or local LAN address")
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
    var isRemoteAiEnabled: Boolean = false
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
