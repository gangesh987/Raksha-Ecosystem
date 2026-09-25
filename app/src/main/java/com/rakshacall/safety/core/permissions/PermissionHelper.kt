package com.rakshacall.safety.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class PermissionDetail(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isRequiredForCore: Boolean
)

object PermissionHelper {

    fun getMicrophonePermission(): String = Manifest.permission.RECORD_AUDIO

    fun getCameraPermission(): String = Manifest.permission.CAMERA

    fun getNotificationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isMicrophoneGranted(context: Context): Boolean {
        return isPermissionGranted(context, getMicrophonePermission())
    }

    fun isCameraGranted(context: Context): Boolean {
        return isPermissionGranted(context, getCameraPermission())
    }

    fun isNotificationGranted(context: Context): Boolean {
        val perm = getNotificationPermission() ?: return true
        return isPermissionGranted(context, perm)
    }

    /**
     * Queries actual real-time Android permission state for all relevant capabilities.
     * Never fabricates 'Granted' unless Android PackageManager reports true.
     */
    fun getAllPermissionsStatus(context: Context): List<PermissionDetail> {
        val list = mutableListOf<PermissionDetail>()

        // 1. Microphone
        val micGranted = isMicrophoneGranted(context)
        list.add(
            PermissionDetail(
                permission = Manifest.permission.RECORD_AUDIO,
                title = "Microphone",
                description = "Used strictly during active protection sessions to recognize spoken phrases on-device and detect coercive tactics. Raw audio is never permanently recorded.",
                isGranted = micGranted,
                isRequiredForCore = true
            )
        )

        // 2. Camera
        val camGranted = isCameraGranted(context)
        list.add(
            PermissionDetail(
                permission = Manifest.permission.CAMERA,
                title = "Camera (Optional)",
                description = "Used to analyze video frame lighting, face presence, and consistency as supporting signals. Visual signals never override conversation intelligence.",
                isGranted = camGranted,
                isRequiredForCore = false
            )
        )

        // 3. Notifications
        val notifGranted = isNotificationGranted(context)
        list.add(
            PermissionDetail(
                permission = getNotificationPermission() ?: "android.permission.NOTIFICATION",
                title = "Notifications",
                description = "Required to display the persistent active safety layer notification and urgent Safety Brake alerts.",
                isGranted = notifGranted,
                isRequiredForCore = true
            )
        )

        return list
    }
}
