package com.rakshacall.safety

import com.rakshacall.safety.core.security.EvidenceHasher
import com.rakshacall.safety.core.security.IntegrityResult
import com.rakshacall.safety.domain.model.EvidenceEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceIntegrityTest {

    @Test
    fun `valid evidence chain verifies as IntegrityResult Valid`() {
        val ev1 = EvidenceHasher.createEvent(
            eventId = "e1",
            sessionId = "s1",
            timestamp = 1000L,
            eventType = "START",
            payloadJson = "{\"status\":\"ACTIVE\"}",
            lastKnownHash = null
        )

        val ev2 = EvidenceHasher.createEvent(
            eventId = "e2",
            sessionId = "s1",
            timestamp = 2000L,
            eventType = "TACTIC",
            payloadJson = "{\"tactic\":\"AUTHORITY_IMPERSONATION\"}",
            lastKnownHash = ev1.currentHash
        )

        val ev3 = EvidenceHasher.createEvent(
            eventId = "e3",
            sessionId = "s1",
            timestamp = 3000L,
            eventType = "SAFETY_BRAKE",
            payloadJson = "{\"triggered\":true}",
            lastKnownHash = ev2.currentHash
        )

        val chain = listOf(ev1, ev2, ev3)
        val result = EvidenceHasher.verifyChain(chain)

        assertTrue(result is IntegrityResult.Valid)
    }

    @Test
    fun `tampered payload in middle of chain immediately causes IntegrityResult Failed`() {
        val ev1 = EvidenceHasher.createEvent(
            eventId = "e1",
            sessionId = "s1",
            timestamp = 1000L,
            eventType = "START",
            payloadJson = "{\"status\":\"ACTIVE\"}",
            lastKnownHash = null
        )

        val ev2 = EvidenceHasher.createEvent(
            eventId = "e2",
            sessionId = "s1",
            timestamp = 2000L,
            eventType = "TACTIC",
            payloadJson = "{\"tactic\":\"AUTHORITY_IMPERSONATION\"}",
            lastKnownHash = ev1.currentHash
        )

        // Attacker attempts to mutate payload of ev2 (e.g. modify tactic text to erase evidence)
        val tamperedEv2 = ev2.copy(payloadJson = "{\"tactic\":\"BENIGN_TALK\"}")

        val ev3 = EvidenceHasher.createEvent(
            eventId = "e3",
            sessionId = "s1",
            timestamp = 3000L,
            eventType = "END",
            payloadJson = "{\"status\":\"COMPLETED\"}",
            lastKnownHash = ev2.currentHash
        )

        val chain = listOf(ev1, tamperedEv2, ev3)
        val result = EvidenceHasher.verifyChain(chain)

        assertTrue(result is IntegrityResult.Failed)
        assertEquals("e2", (result as IntegrityResult.Failed).compromisedEventId)
    }

    @Test
    fun `broken previousHash link immediately fails validation`() {
        val ev1 = EvidenceHasher.createEvent(
            eventId = "e1",
            sessionId = "s1",
            timestamp = 1000L,
            eventType = "START",
            payloadJson = "{}",
            lastKnownHash = null
        )

        // ev2 forged with incorrect previous hash
        val fakeHash = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        val ev2 = EvidenceHasher.createEvent(
            eventId = "e2",
            sessionId = "s1",
            timestamp = 2000L,
            eventType = "TACTIC",
            payloadJson = "{}",
            lastKnownHash = fakeHash
        )

        val chain = listOf(ev1, ev2)
        val result = EvidenceHasher.verifyChain(chain)

        assertTrue(result is IntegrityResult.Failed)
        assertEquals("e2", (result as IntegrityResult.Failed).compromisedEventId)
    }
}
