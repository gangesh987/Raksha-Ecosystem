package com.rakshacall.safety.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PermissionStatus {
    GRANTED,
    DENIED,
    DENIED_PERMANENTLY,
    REVOKED,
    RESTRICTED,
    UNAVAILABLE
}

data class DetailedPermissionSnapshot(
    val microphone: PermissionStatus,
    val camera: PermissionStatus,
    val notifications: PermissionStatus,
    val isProtectionReady: Boolean
)

/**
 * Centralized Permission State Manager tracking real runtime permission lifecycles,
 * reconciling changes when returning from system settings.
 */
class PermissionStateManager(private val context: Context) {

    private val _snapshot = MutableStateFlow(evaluateCurrentPermissions())
    val snapshot: StateFlow<DetailedPermissionSnapshot> = _snapshot.asStateFlow()

    fun refresh() {
        _snapshot.value = evaluateCurrentPermissions()
    }

    private fun evaluateCurrentPermissions(): DetailedPermissionSnapshot {
        val micStatus = checkPermissionStatus(Manifest.permission.RECORD_AUDIO)
        val camStatus = checkPermissionStatus(Manifest.permission.CAMERA)
        val notifStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermissionStatus(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PermissionStatus.GRANTED
        }

        return DetailedPermissionSnapshot(
            microphone = micStatus,
            camera = camStatus,
            notifications = notifStatus,
            isProtectionReady = (micStatus == PermissionStatus.GRANTED)
        )
    }

    private fun checkPermissionStatus(permission: String): PermissionStatus {
        val result = ContextCompat.checkSelfPermission(context, permission)
        return if (result == PackageManager.PERMISSION_GRANTED) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }
}
