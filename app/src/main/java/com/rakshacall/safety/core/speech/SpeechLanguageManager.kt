package com.rakshacall.safety.core.speech

enum class LanguageSupportStatus {
    SUPPORTED,
    EXPERIMENTAL,
    COMING_SOON
}

data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val localizedName: String,
    val status: LanguageSupportStatus
)

/**
 * Manages language capabilities for on-device speech recognition.
 * Only exposes verified working languages to prevent false capabilities.
 */
object SpeechLanguageManager {

    val availableLanguages = listOf(
        SupportedLanguage("en-IN", "English (India)", "English", LanguageSupportStatus.SUPPORTED),
        SupportedLanguage("hi-IN", "Hindi", "हिन्दी", LanguageSupportStatus.SUPPORTED),
        SupportedLanguage("ta-IN", "Tamil", "தமிழ்", LanguageSupportStatus.COMING_SOON),
        SupportedLanguage("te-IN", "Telugu", "తెలుగు", LanguageSupportStatus.COMING_SOON),
        SupportedLanguage("kn-IN", "Kannada", "ಕನ್ನಡ", LanguageSupportStatus.COMING_SOON),
        SupportedLanguage("ml-IN", "Malayalam", "മലയാളം", LanguageSupportStatus.COMING_SOON),
        SupportedLanguage("bn-IN", "Bengali", "বাংলা", LanguageSupportStatus.COMING_SOON),
        SupportedLanguage("mr-IN", "Marathi", "मराठी", LanguageSupportStatus.COMING_SOON)
    )

    @Volatile
    var activeLanguageCode: String = "en-IN"

    fun getActiveLanguage(): SupportedLanguage {
        return availableLanguages.firstOrNull { it.code == activeLanguageCode }
            ?: availableLanguages.first()
    }
}
