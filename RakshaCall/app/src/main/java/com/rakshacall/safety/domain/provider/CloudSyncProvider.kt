package com.rakshacall.safety.domain.provider

import com.rakshacall.safety.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

interface CloudSyncProvider {
    val syncState: Flow<SyncState>
    val isCloudEnabled: Boolean
    suspend fun syncPendingSessions(): Result<Int>
    suspend fun purgeCloudData(userId: String): Result<Unit>
}
