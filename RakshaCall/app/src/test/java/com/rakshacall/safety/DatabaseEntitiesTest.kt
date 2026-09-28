package com.rakshacall.safety

import com.rakshacall.safety.data.local.database.RakshaDatabase
import com.rakshacall.safety.data.local.entities.*
import com.rakshacall.safety.domain.model.EvidenceEvent
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class DatabaseEntitiesTest {

    @Test
    fun testGenesisHashIs64Zeros() {
        val genesis = RakshaDatabase.GENESIS_HASH
        assertEquals(64, genesis.length)
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", genesis)
    }

    @Test
    fun testEvidenceHashChainFormulaCalculation() {
        val prevHash = RakshaDatabase.GENESIS_HASH
        val eventId = "evt-1"
        val timestamp = 1700000000000L
        val eventType = "CALL_STARTED"
        val payload = "{\"source\":\"MICROPHONE\"}"

        val calculated = EvidenceEvent.calculateHash(prevHash, eventId, timestamp, eventType, payload)
        assertNotNull(calculated)
        assertEquals(64, calculated.length) // SHA-256 hex string is 64 chars
        assertTrue(calculated.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test
    fun testEvidenceChainContinuitySuccess() {
        val genesis = RakshaDatabase.GENESIS_HASH
        val event1 = EvidenceEvent(
            eventId = "e1",
            sessionId = "s1",
            timestamp = 1000L,
            eventType = "START",
            payloadJson = "{}",
            previousHash = genesis
        )

        val event2 = EvidenceEvent(
            eventId = "e2",
            sessionId = "s1",
            timestamp = 2000L,
            eventType = "TACTIC",
            payloadJson = "{\"tactic\":\"AUTHORITY\"}",
            previousHash = event1.currentHash
        )

        assertEquals(event1.currentHash, event2.previousHash)
        assertNotEquals(event1.currentHash, event2.currentHash)
    }

    @Test
    fun testTamperingAltersHashChain() {
        val genesis = RakshaDatabase.GENESIS_HASH
        val originalEvent = EvidenceEvent(
            eventId = "e1",
            sessionId = "s1",
            timestamp = 1000L,
            eventType = "RISK_EVAL",
            payloadJson = "{\"risk\":75}",
            previousHash = genesis
        )

        // Attempted tampering: changing risk from 75 to 20
        val tamperedHash = EvidenceEvent.calculateHash(
            genesis, "e1", 1000L, "RISK_EVAL", "{\"risk\":20}"
        )

        assertNotEquals("Tampered content must produce completely different hash", originalEvent.currentHash, tamperedHash)
    }

    @Test
    fun testAll19EntitiesDataModelIntegrity() {
        val now = System.currentTimeMillis()
        val user = UserEntity("u1", "RC-101", "+919876543210", now, "user@test.com", "LOCAL_DEV")
        val session = ProtectionSessionEntity("s1", now, null, "ACTIVE", 0, 0, "CONTACT", "MICROPHONE", false, 0, false)
        val call = CallSessionEntity("c1", "s1", "Inspector Roy", "+919800000000", "WhatsApp", "VIDEO", 45L, "PROTECTED", now)
        val media = MediaSourceEntity("m1", "s1", "LOCAL_MICROPHONE", "STREAMING", 16000, false, now)
        val transcript = TranscriptEventEntity("t1", "s1", now, "CALLER", "Hello", 0.98f, "en-IN")
        val tactic = TacticEventEntity("tac1", "s1", now, "URGENCY", 0.95f, 10, "right now", "LOCAL")
        val stage = ScamStageEventEntity("st1", "s1", now, "CONTACT", "AUTHORITY", "AUTHORITY_IMPERSONATION", "Officer claim")
        val risk = RiskEventEntity("r1", "s1", now, "PAYMENT_DEMAND", 0.9f, 20, "transfer funds", 35)
        val point = RiskPointEntity("s1", now, 35)
        val velocity = VelocityEventEntity("v1", "s1", now, "HIGH", 3, 45.0f, 60000L)
        val contact = TrustedContactEntity("tc1", "Father", "+919999999999", "Father", true, now, "CONSENTED", null)
        val alert = AlertEventEntity("a1", "s1", "tc1", now, "HIGH_RISK_TRIGGER", "ALERT_REQUESTED", "SMS payload")
        val evidence = EvidenceEventEntity("ev1", "s1", now, "PAYMENT", "{}", RakshaDatabase.GENESIS_HASH, "hash1", "LOCAL_ONLY", null, 1)
        val consent = ConsentRecordEntity("cs1", "RECORD_AUDIO", now, null, "2.0.0")
        val verification = VerificationEventEntity("vr1", "s1", now, 1, "PAUSE", "VERIFIED", "Paused call")
        val platform = PlatformConnectionEntity("p1", "WhatsApp", "Microphone", true, "GRANTED", now)
        val aiEvent = AIEventEntity("ai1", "s1", now, "LOCAL", "NLP-v2", 12L, "SUCCESS", null)
        val report = IncidentReportEntity("rep1", "s1", now, "{}", "Text report", "VERIFIED", "JSON_AND_TEXT")
        val notif = NotificationEventEntity("n1", now, "Risk Alert", "High risk detected", "HIGH", "RISK_ALERT", false)

        assertEquals("u1", user.id)
        assertEquals("s1", session.id)
        assertEquals("c1", call.id)
        assertEquals("m1", media.id)
        assertEquals("t1", transcript.id)
        assertEquals("tac1", tactic.id)
        assertEquals("st1", stage.id)
        assertEquals("r1", risk.id)
        assertEquals(35, point.riskScore)
        assertEquals("HIGH", velocity.velocityLevel)
        assertEquals("Father", contact.name)
        assertEquals("ALERT_REQUESTED", alert.deliveryStatus)
        assertEquals("LOCAL_ONLY", evidence.syncState)
        assertEquals("RECORD_AUDIO", consent.permissionName)
        assertEquals("VERIFIED", verification.status)
        assertTrue(platform.isSupported)
        assertEquals("SUCCESS", aiEvent.status)
        assertEquals("VERIFIED", report.integrityStatus)
        assertFalse(notif.isRead)
    }

    @Test
    fun testCallSessionDurationCalculation() {
        val call = CallSessionEntity(
            id = UUID.randomUUID().toString(),
            sessionId = "sess-100",
            callerName = "Mumbai Customs",
            callerNumber = "+91 22 2000 0000",
            platform = "Google Meet",
            callType = "VIDEO",
            durationSeconds = 185L,
            outcome = "INTERRUPTED_BY_SAFETY_BRAKE"
        )
        assertEquals(185L, call.durationSeconds)
        assertEquals("INTERRUPTED_BY_SAFETY_BRAKE", call.outcome)
    }

    @Test
    fun testNotificationEventHighPriorityClassification() {
        val alertNotif = NotificationEventEntity(
            id = "n-99",
            timestamp = System.currentTimeMillis(),
            title = "CRITICAL RISK",
            body = "Safety brake activated for session s1",
            priority = "HIGH",
            category = "SAFETY_BRAKE",
            isRead = false
        )
        assertEquals("HIGH", alertNotif.priority)
        assertEquals("SAFETY_BRAKE", alertNotif.category)
        assertFalse(alertNotif.isRead)
    }
}
