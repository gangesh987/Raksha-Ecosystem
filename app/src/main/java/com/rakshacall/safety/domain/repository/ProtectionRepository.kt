package com.rakshacall.safety.domain.repository

import com.rakshacall.safety.domain.model.ConsentRecord
import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.TrustedContact

/**
 * Offline-First Room-to-Firestore repository interface synchronizing protection events,
 * sessions, evidence chains, and audit records.
 */
interface ProtectionRepository {
    suspend fun syncPendingEvents(userId: String): Int
    suspend fun syncSessionToFirestore(userId: String, session: ProtectionSession): Boolean
    suspend fun syncEvidenceToFirestore(userId: String, events: List<EvidenceEvent>): Boolean
    suspend fun syncTrustedContactsToFirestore(userId: String, contacts: List<TrustedContact>): Boolean
    suspend fun syncConsentRecordToFirestore(userId: String, consent: ConsentRecord): Boolean
}
