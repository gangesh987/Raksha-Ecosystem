package com.rakshacall.safety.core.security

import java.security.MessageDigest

/**
 * Standard cryptographic hashing helpers using SHA-256.
 */
object CryptographyHelper {

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate a dynamic RakshaCall user ID: RC-XXXXXX (6 hex/alphanumeric chars)
     */
    fun generateRakshaCallId(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" // unambiguous chars
        val randomStr = (1..6).map { chars.random() }.joinToString("")
        return "RC-$randomStr"
    }

    /**
     * Generate a local 6-digit OTP for prototype verification
     */
    fun generateLocalOtp(): String {
        val code = (100000..999999).random()
        return code.toString()
    }
}
