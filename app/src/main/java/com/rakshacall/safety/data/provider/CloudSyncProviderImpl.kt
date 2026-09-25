package com.rakshacall.safety.data.provider

import com.rakshacall.safety.data.sync.SyncManager
import com.rakshacall.safety.domain.model.SyncState
import com.rakshacall.safety.domain.provider.CloudSyncProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CloudSyncProviderImpl(
    private val syncManager: SyncManager,
    private val cloudEnabled: Boolean = false
) : CloudSyncProvider {

    private val _syncState = MutableStateFlow(if (cloudEnabled) SyncState.SYNCED else SyncState.LOCAL_ONLY)
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    override val isCloudEnabled: Boolean = cloudEnabled

    override suspend fun syncPendingSessions(): Result<Int> {
        if (!cloudEnabled) {
            _syncState.value = SyncState.LOCAL_ONLY
            return Result.success(0)
        }
        _syncState.value = SyncState.SYNCING
        syncManager.triggerSync("all_pending")
        _syncState.value = SyncState.SYNCED
        return Result.success(1)

    }

    override suspend fun purgeCloudData(userId: String): Result<Unit> {
        _syncState.value = SyncState.LOCAL_ONLY
        return Result.success(Unit)
    }
}
