package com.rakshacall.safety.core.security

import com.rakshacall.safety.domain.model.EvidenceEvent

/**
 * Result of validating an evidence chain.
 */
sealed class IntegrityResult {
    data object Valid : IntegrityResult()
    data class Failed(val compromisedEventId: String, val reason: String) : IntegrityResult()
    data object Empty : IntegrityResult()
}

/**
 * Tamper-evident append-only hash chain calculator and verifier.
 * Every event contains a cryptographic link to the previous event's hash.
 */
object EvidenceHasher {

    const val GENESIS_PREVIOUS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

    /**
     * Compute currentHash for an event given its predecessor's hash.
     * Formula: SHA-256(previousHash + ":" + eventId + ":" + timestamp + ":" + eventType + ":" + sessionId + ":" + payloadJson)
     */
    fun computeHash(
        previousHash: String,
        eventId: String,
        timestamp: Long,
        eventType: String,
        sessionId: String,
        payloadJson: String
    ): String {
        val raw = "$previousHash:$eventId:$timestamp:$eventType:$sessionId:$payloadJson"
        return CryptographyHelper.sha256(raw)
    }

    /**
     * Create an EvidenceEvent calculating its cryptographic hash.
     */
    fun createEvent(
        eventId: String,
        sessionId: String,
        timestamp: Long,
        eventType: String,
        payloadJson: String,
        lastKnownHash: String?
    ): EvidenceEvent {
        val prevHash = lastKnownHash ?: GENESIS_PREVIOUS_HASH
        val currHash = computeHash(prevHash, eventId, timestamp, eventType, sessionId, payloadJson)
        return EvidenceEvent(
            eventId = eventId,
            sessionId = sessionId,
            timestamp = timestamp,
            eventType = eventType,
            payloadJson = payloadJson,
            previousHash = prevHash,
            currentHash = currHash
        )
    }

    /**
     * Verify the entire chain of evidence events sequentially.
     * Returns IntegrityResult.Valid if every hash matches the expected recalculated hash
     * and correctly links to the previous event's hash.
     */
    fun verifyChain(events: List<EvidenceEvent>): IntegrityResult {
        if (events.isEmpty()) return IntegrityResult.Empty

        var expectedPreviousHash = GENESIS_PREVIOUS_HASH

        for ((index, event) in events.withIndex()) {
            if (index == 0 && event.previousHash != GENESIS_PREVIOUS_HASH) {
                return IntegrityResult.Failed(
                    compromisedEventId = event.eventId,
                    reason = "Genesis event has invalid previousHash: ${event.previousHash}"
                )
            }

            if (index > 0 && event.previousHash != expectedPreviousHash) {
                return IntegrityResult.Failed(
                    compromisedEventId = event.eventId,
                    reason = "Broken chain link: expected previousHash $expectedPreviousHash but found ${event.previousHash}"
                )
            }

            val recalculatedHash = computeHash(
                previousHash = event.previousHash,
                eventId = event.eventId,
                timestamp = event.timestamp,
                eventType = event.eventType,
                sessionId = event.sessionId,
                payloadJson = event.payloadJson
            )

            if (recalculatedHash != event.currentHash) {
                return IntegrityResult.Failed(
                    compromisedEventId = event.eventId,
                    reason = "Payload or metadata altered. Computed hash $recalculatedHash does not match recorded ${event.currentHash}"
                )
            }

            expectedPreviousHash = event.currentHash
        }

        return IntegrityResult.Valid
    }
}
