package com.rakshacall.safety.data.remote.config

/**
 * Dynamic configuration management.
 * Avoids hardcoded production URLs and supports environment switching.
 */
object AppConfig {

    const val APP_VERSION = "2.0.0-PROD"
    const val DEFAULT_BASE_URL = "https://api.rakshacall.org"

    @Volatile
    var activeBaseUrl: String = DEFAULT_BASE_URL

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
