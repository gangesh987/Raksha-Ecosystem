package com.rakshacall.safety.data.firebase

import android.content.Context
import com.rakshacall.safety.core.notifications.NotificationHelper
import com.rakshacall.safety.core.security.SecurityLogger

/**
 * FCM Push Notification handler mapping notification categories
 * without leaking sensitive transcripts or PII to the lock screen.
 */
object FirebaseNotificationHandler {

    enum class NotificationCategory {
        PROTECTION_ACTIVE,
        HIGH_RISK,
        SAFETY_BRAKE,
        TRUSTED_CONTACT,
        SYNC_STATUS,
        SYSTEM
    }

    fun handleIncomingPayload(context: Context, data: Map<String, String>) {
        val categoryStr = data["category"] ?: NotificationCategory.SYSTEM.name
        val category = try {
            NotificationCategory.valueOf(categoryStr)
        } catch (e: Exception) {
            NotificationCategory.SYSTEM
        }

        // Privacy-conscious sanitized messaging: Never include raw OTP or transcript text on lockscreen
        val sanitizedTitle = when (category) {
            NotificationCategory.PROTECTION_ACTIVE -> "RakshaCall Active"
            NotificationCategory.HIGH_RISK -> "RakshaCall Security Notice"
            NotificationCategory.SAFETY_BRAKE -> "CRITICAL SAFETY INTERVENTION"
            NotificationCategory.TRUSTED_CONTACT -> "Trusted Contact Alert"
            NotificationCategory.SYNC_STATUS -> "Cloud Sync Update"
            NotificationCategory.SYSTEM -> "System Notification"
        }

        val sanitizedMessage = when (category) {
            NotificationCategory.SAFETY_BRAKE -> "High coercion pattern detected. Please pause the call and verify independently."
            NotificationCategory.HIGH_RISK -> "Suspicious patterns detected during active interaction."
            NotificationCategory.PROTECTION_ACTIVE -> "On-device safety layer is monitoring audio."
            else -> "RakshaCall status update."
        }

        SecurityLogger.info("Dispatching privacy-safe push notification for category: $category")
        NotificationHelper.showNotification(
            context = context,
            title = sanitizedTitle,
            message = sanitizedMessage,
            isHighPriority = (category == NotificationCategory.SAFETY_BRAKE || category == NotificationCategory.HIGH_RISK)
        )
    }
}
