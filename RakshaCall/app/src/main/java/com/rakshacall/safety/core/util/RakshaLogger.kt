package com.rakshacall.safety.core.util

/**
 * JVM-safe logger that emits to Android Logcat when running on device/emulator,
 * and falls back to standard output in host unit tests.
 */
object RakshaLogger {
    fun e2e(eventTag: String, message: String) {
        try {
            android.util.Log.i("RakshaE2E", "[$eventTag] $message")
        } catch (_: Throwable) {
            println("[RakshaE2E] [$eventTag] $message")
        }
    }
}
