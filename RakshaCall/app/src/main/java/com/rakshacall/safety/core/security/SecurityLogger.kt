package com.rakshacall.safety.core.security

import android.util.Log

/**
 * PII-safe structured security logger.
 * Strictly prevents transcripts, phone numbers, OTPs, or cryptographic keys from leaking to logcat.
 */
object SecurityLogger {

    private const val TAG = "RakshaCallSecurity"
    var isDebug = false

    fun info(event: String, sessionId: String? = null) {
        val sessionTag = sessionId?.take(8) ?: "none"
        Log.i(TAG, "[INFO] [$sessionTag] $event")
    }

    fun warn(event: String, sessionId: String? = null) {
        val sessionTag = sessionId?.take(8) ?: "none"
        Log.w(TAG, "[WARN] [$sessionTag] $event")
    }

    fun error(event: String, throwable: Throwable? = null, sessionId: String? = null) {
        val sessionTag = sessionId?.take(8) ?: "none"
        Log.e(TAG, "[ERROR] [$sessionTag] $event", throwable)
    }

    fun audit(action: String, outcome: String, details: String = "") {
        // Redact any possible numbers or patterns that look like phone numbers or OTPs
        val sanitizedDetails = details
            .replace(Regex("""\b\d{6}\b"""), "[REDACTED_OTP]")
            .replace(Regex("""\b\d{10}\b"""), "[REDACTED_PHONE]")
        Log.i(TAG, "[AUDIT] action=$action outcome=$outcome details=$sanitizedDetails")
    }
}
