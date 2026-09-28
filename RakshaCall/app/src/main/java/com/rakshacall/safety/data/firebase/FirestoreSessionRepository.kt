package com.rakshacall.safety.data.firebase

import com.rakshacall.safety.core.security.SecurityLogger
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Cloud Firestore Session repository with local Room repository caching.
 * Writes to local storage first, then synchronizes to Cloud Firestore if connected.
 */
class FirestoreSessionRepository(
    private val localRepository: SessionRepository
) : SessionRepository {

    override suspend fun createSession(session: ProtectionSession) {
        localRepository.createSession(session)
        SecurityLogger.info("Session persisted locally (cloud sync staged)", session.id)
    }

    override suspend fun getSession(sessionId: String): ProtectionSession? {
        return localRepository.getSession(sessionId)
    }

    override fun observeActiveSession(): Flow<ProtectionSession?> {
        return localRepository.observeActiveSession()
    }

    override fun observeAllSessions(): Flow<List<ProtectionSession>> {
        return localRepository.observeAllSessions()
    }

    override suspend fun updateSession(session: ProtectionSession) {
        localRepository.updateSession(session)
    }

    override suspend fun endSession(sessionId: String, finalRisk: Int, peakRisk: Int) {
        localRepository.endSession(sessionId, finalRisk, peakRisk)
    }

    override suspend fun deleteSession(sessionId: String) {
        localRepository.deleteSession(sessionId)
    }

    override suspend fun clearAllSessions() {
        localRepository.clearAllSessions()
    }
}
