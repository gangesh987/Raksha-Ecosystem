package com.rakshacall.safety.data.repository

import com.rakshacall.safety.data.local.database.RakshaDatabase
import com.rakshacall.safety.data.local.entities.EvidenceEventEntity
import com.rakshacall.safety.data.local.entities.ProtectionSessionEntity
import com.rakshacall.safety.data.local.entities.RiskEventEntity
import com.rakshacall.safety.data.local.entities.TranscriptEventEntity
import com.rakshacall.safety.data.local.entities.TrustedContactEntity
import com.rakshacall.safety.data.local.entities.UserEntity
import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.SessionStatus
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.domain.model.TrustedContact
import com.rakshacall.safety.domain.model.User
import com.rakshacall.safety.domain.repository.EvidenceRepository
import com.rakshacall.safety.domain.repository.RiskRepository
import com.rakshacall.safety.domain.repository.SessionRepository
import com.rakshacall.safety.domain.repository.TrustedContactRepository
import com.rakshacall.safety.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalUserRepository(private val database: RakshaDatabase) : UserRepository {
    override suspend fun getActiveUser(): User? = database.getUser()?.toDomain()

    override fun observeActiveUser(): Flow<User?> = database.observeUser().map { it?.toDomain() }

    override suspend fun saveUser(user: User) {
        database.insertUser(UserEntity(user.id, user.rakshaCallId, user.phoneNumber, user.createdAt))
    }

    override suspend fun clearUser() {
        database.deleteUser()
    }

    private fun UserEntity.toDomain() = User(id, rakshaCallId, phoneNumber, createdAt)
}

class LocalSessionRepository(private val database: RakshaDatabase) : SessionRepository {
    override suspend fun createSession(session: ProtectionSession) {
        database.insertSession(
            ProtectionSessionEntity(
                id = session.id,
                startTime = session.startTime,
                endTime = session.endTime,
                status = session.status.name,
                peakRisk = session.peakRisk,
                finalRisk = session.finalRisk,
                highestStage = session.highestStage.name,
                inputSource = session.inputSource,
                safetyBrakeTriggered = session.safetyBrakeTriggered,
                totalTacticsDetected = session.totalTacticsDetected,
                isDemoSession = session.isDemoSession
            )
        )
    }

    override suspend fun getSession(sessionId: String): ProtectionSession? {
        return database.getSession(sessionId)?.toDomain()
    }

    override fun observeActiveSession(): Flow<ProtectionSession?> {
        return database.observeSessions().map { list ->
            list.firstOrNull { it.status == "ACTIVE" }?.toDomain()
        }
    }

    override fun observeAllSessions(): Flow<List<ProtectionSession>> {
        return database.observeSessions().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun updateSession(session: ProtectionSession) {
        createSession(session)
    }

    override suspend fun endSession(sessionId: String, finalRisk: Int, peakRisk: Int) {
        val existing = database.getSession(sessionId)
        if (existing != null) {
            database.insertSession(
                existing.copy(
                    endTime = System.currentTimeMillis(),
                    status = "COMPLETED",
                    finalRisk = finalRisk,
                    peakRisk = maxOf(existing.peakRisk, peakRisk)
                )
            )
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        database.deleteSession(sessionId)
    }

    override suspend fun clearAllSessions() {
        database.clearAllData()
    }

    private fun ProtectionSessionEntity.toDomain() = ProtectionSession(
        id = id,
        startTime = startTime,
        endTime = endTime,
        status = runCatching { SessionStatus.valueOf(status) }.getOrDefault(SessionStatus.COMPLETED),
        peakRisk = peakRisk,
        finalRisk = finalRisk,
        highestStage = runCatching { ScamStage.valueOf(highestStage) }.getOrDefault(ScamStage.CONTACT),
        inputSource = inputSource,
        safetyBrakeTriggered = safetyBrakeTriggered,
        totalTacticsDetected = totalTacticsDetected,
        isDemoSession = isDemoSession
    )
}

class LocalRiskRepository(private val database: RakshaDatabase) : RiskRepository {
    override suspend fun insertTranscriptEvent(event: TranscriptEvent) {
        database.insertTranscript(
            TranscriptEventEntity(
                id = event.id,
                sessionId = event.sessionId,
                timestamp = event.timestamp,
                speaker = event.speaker,
                text = event.text,
                confidence = event.confidence
            )
        )
    }

    override fun observeTranscripts(sessionId: String): Flow<List<TranscriptEvent>> {
        return database.observeTranscripts(sessionId).map { list ->
            list.map { TranscriptEvent(it.id, it.sessionId, it.timestamp, it.speaker, it.text, it.confidence) }
        }
    }

    override suspend fun getTranscripts(sessionId: String): List<TranscriptEvent> {
        return database.getTranscripts(sessionId).map {
            TranscriptEvent(it.id, it.sessionId, it.timestamp, it.speaker, it.text, it.confidence)
        }
    }

    override suspend fun insertRiskSignal(signal: RiskSignal) {
        database.insertRiskEvent(
            RiskEventEntity(
                id = signal.id,
                sessionId = signal.sessionId,
                timestamp = signal.timestamp,
                tactic = signal.tactic.name,
                confidence = signal.confidence,
                riskContribution = signal.riskContribution,
                evidenceText = signal.evidenceText,
                cumulativeRisk = 0
            )
        )
    }

    override fun observeRiskSignals(sessionId: String): Flow<List<RiskSignal>> {
        // Query current risk signals
        return kotlinx.coroutines.flow.flow {
            emit(getRiskSignals(sessionId))
        }
    }

    override suspend fun getRiskSignals(sessionId: String): List<RiskSignal> {
        return database.getRiskEvents(sessionId).map {
            RiskSignal(
                id = it.id,
                timestamp = it.timestamp,
                tactic = runCatching { ScamTactic.valueOf(it.tactic) }.getOrDefault(ScamTactic.AUTHORITY_IMPERSONATION),
                confidence = it.confidence,
                riskContribution = it.riskContribution,
                evidenceText = it.evidenceText,
                sessionId = it.sessionId
            )
        }
    }

    override suspend fun recordRiskPoint(sessionId: String, timestamp: Long, riskScore: Int) {
        database.insertRiskPoint(sessionId, timestamp, riskScore)
    }

    override fun observeRiskPoints(sessionId: String): Flow<List<Pair<Long, Int>>> {
        return database.observeRiskPoints(sessionId)
    }

    override suspend fun getRiskPoints(sessionId: String): List<Pair<Long, Int>> {
        return database.getRiskPoints(sessionId)
    }
}

class LocalEvidenceRepository(private val database: RakshaDatabase) : EvidenceRepository {
    override suspend fun appendEvidence(event: EvidenceEvent) {
        database.insertEvidence(
            EvidenceEventEntity(
                eventId = event.eventId,
                sessionId = event.sessionId,
                timestamp = event.timestamp,
                eventType = event.eventType,
                payloadJson = event.payloadJson,
                previousHash = event.previousHash,
                currentHash = event.currentHash
            )
        )
    }

    override fun observeEvidence(sessionId: String): Flow<List<EvidenceEvent>> {
        return kotlinx.coroutines.flow.flow {
            emit(getEvidenceForSession(sessionId))
        }
    }

    override suspend fun getEvidenceForSession(sessionId: String): List<EvidenceEvent> {
        return database.getEvidenceForSession(sessionId).map {
            EvidenceEvent(
                eventId = it.eventId,
                sessionId = it.sessionId,
                timestamp = it.timestamp,
                eventType = it.eventType,
                payloadJson = it.payloadJson,
                previousHash = it.previousHash,
                currentHash = it.currentHash
            )
        }
    }

    override suspend fun getLastEvidenceHash(sessionId: String): String? {
        return database.getLastEvidenceHash(sessionId)
    }

    override suspend fun clearAllEvidence() {
        database.clearAllData()
    }
}

class LocalTrustedContactRepository(private val database: RakshaDatabase) : TrustedContactRepository {
    override fun observeContacts(): Flow<List<TrustedContact>> {
        return database.observeContacts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllContacts(): List<TrustedContact> {
        return database.getAllContacts().map { it.toDomain() }
    }

    override suspend fun addContact(contact: TrustedContact) {
        database.insertContact(
            TrustedContactEntity(
                id = contact.id,
                name = contact.name,
                phoneNumber = contact.phoneNumber,
                relationship = contact.relationship,
                isEmergency = contact.isEmergency,
                createdAt = contact.createdAt
            )
        )
    }

    override suspend fun updateContact(contact: TrustedContact) {
        addContact(contact)
    }

    override suspend fun deleteContact(contactId: String) {
        database.deleteContact(contactId)
    }

    override suspend fun clearAllContacts() {
        database.clearAllData()
    }

    private fun TrustedContactEntity.toDomain() = TrustedContact(id, name, phoneNumber, relationship, isEmergency, createdAt)
}
