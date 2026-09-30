package com.rakshacall.safety.protection

/**
 * 4 Protection Modes defined in Section 2:
 * 1. MONITOR: Analyze available signals. Do not interrupt call. Display risk.
 * 2. WARN: Analyze continuously. Display warnings. Provide Mute, End Call, Trusted Contact, Save Evidence.
 * 3. STRONG_PROTECTION: When risk becomes high, display persistent warning. Require explicit user confirmation before sensitive actions.
 * 4. AUTO_PROTECT: For extremely high-confidence dangerous interactions, triggers countdown to termination. User must explicitly enable.
 */
enum class ProtectionMode {
    MONITOR,
    WARN,
    STRONG_PROTECTION,
    AUTO_PROTECT
}

/**
 * Evidence Strength Classification (Section 19):
 * WEAK, MODERATE, STRONG, VERY_STRONG.
 * High or Critical intervention requires multiple independent signals.
 */
enum class EvidenceStrength {
    WEAK,
    MODERATE,
    STRONG,
    VERY_STRONG
}

/**
 * Emergency Protection Controller States (Section 5):
 * NORMAL -> MONITORING -> WARNING -> HIGH_RISK -> CRITICAL -> PROTECTION_PENDING -> PROTECTED -> USER_OVERRIDE -> CALL_ENDED.
 */
enum class ProtectionState {
    NORMAL,
    MONITORING,
    WARNING,
    HIGH_RISK,
    CRITICAL,
    PROTECTION_PENDING,
    PROTECTED,
    USER_OVERRIDE,
    CALL_ENDED
}

/**
 * Display modes (Section 31 & 32):
 * - SIMPLE_ELDERLY: Large text, large emergency buttons, clear warning, high contrast, minimal jargon.
 * - ADVANCED: Detailed dashboard (score, confidence, tactics, stage, velocity, visual signals, evidence timeline).
 */
enum class UIMode {
    ADVANCED,
    SIMPLE_ELDERLY
}

/**
 * Configurable Protection Policy (Section 1 & 4).
 * AUTO_PROTECT is strictly OFF by default and requires explicit user activation.
 */
data class ProtectionPolicy(
    val mode: ProtectionMode = ProtectionMode.STRONG_PROTECTION,
    val autoProtectEnabled: Boolean = false,
    val countdownSeconds: Int = 5,
    val requireMultipleIndependentSignals: Boolean = true,
    val uiMode: UIMode = UIMode.ADVANCED
)
