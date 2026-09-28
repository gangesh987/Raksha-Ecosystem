package com.rakshacall.safety.domain.repository

import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.domain.model.TrustedContact
import com.rakshacall.safety.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getActiveUser(): User?
    fun observeActiveUser(): Flow<User?>
    suspend fun saveUser(user: User)
    suspend fun clearUser()
}

interface SessionRepository {
    suspend fun createSession(session: ProtectionSession)
    suspend fun getSession(sessionId: String): ProtectionSession?
    fun observeActiveSession(): Flow<ProtectionSession?>
    fun observeAllSessions(): Flow<List<ProtectionSession>>
    suspend fun updateSession(session: ProtectionSession)
    suspend fun endSession(sessionId: String, finalRisk: Int, peakRisk: Int)
    suspend fun deleteSession(sessionId: String)
    suspend fun clearAllSessions()
}

interface RiskRepository {
    suspend fun insertTranscriptEvent(event: TranscriptEvent)
    fun observeTranscripts(sessionId: String): Flow<List<TranscriptEvent>>
    suspend fun getTranscripts(sessionId: String): List<TranscriptEvent>

    suspend fun insertRiskSignal(signal: RiskSignal)
    fun observeRiskSignals(sessionId: String): Flow<List<RiskSignal>>
    suspend fun getRiskSignals(sessionId: String): List<RiskSignal>

    suspend fun recordRiskPoint(sessionId: String, timestamp: Long, riskScore: Int)
    fun observeRiskPoints(sessionId: String): Flow<List<Pair<Long, Int>>>
    suspend fun getRiskPoints(sessionId: String): List<Pair<Long, Int>>
}

interface EvidenceRepository {
    suspend fun appendEvidence(event: EvidenceEvent)
    fun observeEvidence(sessionId: String): Flow<List<EvidenceEvent>>
    suspend fun getEvidenceForSession(sessionId: String): List<EvidenceEvent>
    suspend fun getLastEvidenceHash(sessionId: String): String?
    suspend fun clearAllEvidence()
}

interface TrustedContactRepository {
    fun observeContacts(): Flow<List<TrustedContact>>
    suspend fun getAllContacts(): List<TrustedContact>
    suspend fun addContact(contact: TrustedContact)
    suspend fun updateContact(contact: TrustedContact)
    suspend fun deleteContact(contactId: String)
    suspend fun clearAllContacts()
}
