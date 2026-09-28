package com.rakshacall.safety.data.sync

import com.rakshacall.safety.core.security.SecurityLogger
import com.rakshacall.safety.data.remote.client.NetworkClient
import com.rakshacall.safety.data.remote.config.AppConfig
import com.rakshacall.safety.data.remote.dto.SessionEventRequest
import com.rakshacall.safety.data.remote.dto.SyncRequest
import com.rakshacall.safety.domain.model.SyncState
import com.rakshacall.safety.domain.repository.EvidenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncQueueStatus {
    OFFLINE_ONLY,
    IDLE_SYNCED,
    PENDING_CHANGES,
    SYNCING,
    SYNC_FAILED
}

/**
 * Offline-first synchronization manager.
 * Local SQLite/Room remains the absolute source of truth.
 * Protection sessions and evidence chains are never blocked by network latency or absence.
 */
class SyncManager(
    private val evidenceRepository: EvidenceRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val _syncStatus = MutableStateFlow(SyncQueueStatus.OFFLINE_ONLY)
    val syncStatus: StateFlow<SyncQueueStatus> = _syncStatus.asStateFlow()

    private val _pendingItemCount = MutableStateFlow(0)
    val pendingItemCount: StateFlow<Int> = _pendingItemCount.asStateFlow()

    fun triggerSync(sessionId: String) {
        if (!AppConfig.isCloudSyncEnabled) {
            _syncStatus.value = SyncQueueStatus.OFFLINE_ONLY
            return
        }

        scope.launch {
            try {
                _syncStatus.value = SyncQueueStatus.SYNCING
                val evidenceEvents = evidenceRepository.getEvidenceForSession(sessionId)
                if (evidenceEvents.isEmpty()) {
                    _syncStatus.value = SyncQueueStatus.IDLE_SYNCED
                    return@launch
                }

                val dtos = evidenceEvents.map { ev ->
                    SessionEventRequest(
                        eventId = ev.eventId,
                        sessionId = ev.sessionId,
                        timestamp = ev.timestamp,
                        eventType = ev.eventType,
                        payloadJson = ev.payloadJson,
                        previousHash = ev.previousHash,
                        currentHash = ev.currentHash
                    )
                }

                val request = SyncRequest(
                    lastSyncedTimestamp = System.currentTimeMillis() - 86400000L,
                    pendingEvents = dtos
                )

                val response = NetworkClient.apiService.syncPendingEvents(request)
                if (response.isSuccessful) {
                    _syncStatus.value = SyncQueueStatus.IDLE_SYNCED
                    _pendingItemCount.value = 0
                    SecurityLogger.info("Successfully synced ${dtos.size} evidence events", sessionId)
                } else {
                    _syncStatus.value = SyncQueueStatus.SYNC_FAILED
                    SecurityLogger.warn("Cloud sync rejected with code ${response.code()}", sessionId)
                }
            } catch (e: Exception) {
                // Offline fallback: protection continues unhindered
                _syncStatus.value = SyncQueueStatus.PENDING_CHANGES
                SecurityLogger.info("Network unavailable for sync; evidence safely preserved locally", sessionId)
            }
        }
    }
}
