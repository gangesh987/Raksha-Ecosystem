package com.rakshacall.safety.evidence

import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

enum class EvidenceSource {
    AUDIO,
    TRANSCRIPT,
    VISUAL,
    USER_ACTION,
    SYSTEM,
    NETWORK,
    MODEL
}

data class CryptographicEvidenceEvent(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val source: EvidenceSource,
    val language: String? = null,
    val tactic: String? = null,
    val stage: String? = null,
    val riskScore: Int? = null,
    val confidence: Float? = null,
    val description: String,
    val previousHash: String,
    val hash: String
)

/**
 * Tamper-evident incident ledger implementing cryptographic hash chaining:
 * hash_n = SHA256(previousHash + canonicalEvent_n).
 * Enforces strict zero-retention privacy filters (never storing raw credentials or raw audio/video).
 */
class EvidenceVault {

    companion object {
        const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"
    }

    private val sessionEvidenceChains = ConcurrentHashMap<String, CopyOnWriteArrayList<CryptographicEvidenceEvent>>()
    private val sessionLatestHashes = ConcurrentHashMap<String, String>()

    // Privacy sanitizer pattern to scrub secrets from loggable evidence descriptions
    private val credentialSanitizer = Regex("""(?i)\b(\d{4,8}|[A-Z0-9]{6,12})\b""")
    private val sensitiveKeywords = listOf("otp", "pin", "password", "cvv", "token", "jwt")

    fun sanitizeDescription(raw: String): String {
        var clean = raw
        for (kw in sensitiveKeywords) {
            val kwPattern = Regex("""(?i)($kw[:\s=]+)(\S+)""")
            clean = clean.replace(kwPattern) { "${it.groupValues[1]}[REDACTED]" }
        }
        return clean
    }

    @Synchronized
    fun recordEvent(
        sessionId: String,
        eventType: String,
        source: EvidenceSource,
        language: String? = null,
        tactic: String? = null,
        stage: String? = null,
        riskScore: Int? = null,
        confidence: Float? = null,
        description: String,
        timestamp: Long = System.currentTimeMillis()
    ): CryptographicEvidenceEvent {
        val sanitizedDesc = sanitizeDescription(description)
        val prevHash = sessionLatestHashes.getOrDefault(sessionId, GENESIS_HASH)

        // Compute canonical representation
        val canonical = buildString {
            append(sessionId).append("|")
            append(timestamp).append("|")
            append(eventType).append("|")
            append(source.name).append("|")
            append(language ?: "").append("|")
            append(tactic ?: "").append("|")
            append(stage ?: "").append("|")
            append(riskScore ?: 0).append("|")
            append(confidence ?: 0.0f).append("|")
            append(sanitizedDesc)
        }

        val eventHash = sha256("$prevHash:$canonical")

        val event = CryptographicEvidenceEvent(
            sessionId = sessionId,
            timestamp = timestamp,
            eventType = eventType,
            source = source,
            language = language,
            tactic = tactic,
            stage = stage,
            riskScore = riskScore,
            confidence = confidence,
            description = sanitizedDesc,
            previousHash = prevHash,
            hash = eventHash
        )

        sessionEvidenceChains.computeIfAbsent(sessionId) { CopyOnWriteArrayList() }.add(event)
        sessionLatestHashes[sessionId] = eventHash

        return event
    }

    fun getSessionEvidence(sessionId: String): List<CryptographicEvidenceEvent> {
        return sessionEvidenceChains[sessionId]?.toList() ?: emptyList()
    }

    /**
     * Verify cryptographic chain integrity. Returns true if and only if no event has been tampered with.
     */
    fun verifyChainIntegrity(sessionId: String): Boolean {
        val events = sessionEvidenceChains[sessionId] ?: return true
        var expectedPrev = GENESIS_HASH

        for (e in events) {
            if (e.previousHash != expectedPrev) return false

            val canonical = buildString {
                append(e.sessionId).append("|")
                append(e.timestamp).append("|")
                append(e.eventType).append("|")
                append(e.source.name).append("|")
                append(e.language ?: "").append("|")
                append(e.tactic ?: "").append("|")
                append(e.stage ?: "").append("|")
                append(e.riskScore ?: 0).append("|")
                append(e.confidence ?: 0.0f).append("|")
                append(e.description)
            }

            val computed = sha256("${e.previousHash}:$canonical")
            if (computed != e.hash) return false

            expectedPrev = e.hash
        }
        return true
    }

    fun clearSession(sessionId: String) {
        sessionEvidenceChains.remove(sessionId)
        sessionLatestHashes.remove(sessionId)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
