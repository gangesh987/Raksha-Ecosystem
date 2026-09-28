package com.rakshacall.safety.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.rakshacall.MainActivity

object NotificationHelper {

    const val CHANNEL_PROTECTION_ID = "rakshacall_protection_channel"
    const val CHANNEL_ALERT_ID = "rakshacall_safety_alerts_channel"

    const val NOTIFICATION_PROTECTION_ID = 1001
    const val NOTIFICATION_SAFETY_BRAKE_ID = 1002

    const val ACTION_STOP_PROTECTION = "com.rakshacall.safety.ACTION_STOP_PROTECTION"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val protectionChannel = NotificationChannel(
                CHANNEL_PROTECTION_ID,
                "Protection Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when RakshaCall active protection is monitoring."
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Safety Brake Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alerts when coercive scam behavior is identified."
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(protectionChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun buildProtectionNotification(context: Context, statusText: String = "RakshaCall protection is active."): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(ACTION_STOP_PROTECTION).apply {
            setPackage(context.packageName)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_PROTECTION_ID)
            .setContentTitle("RakshaCall Active Protection")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP PROTECTION", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun showSafetyBrakeNotification(context: Context, tacticName: String, riskScore: Int) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SAFETY_BRAKE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT_ID)
            .setContentTitle("CRITICAL ALERT: Safety Brake Activated ($riskScore/100)")
            .setContentText("Detected $tacticName. Do not transfer funds or disclose credentials.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_SAFETY_BRAKE_ID, notification)
    }

    fun showNotification(context: Context, title: String, message: String, isHighPriority: Boolean = false) {
        val channelId = if (isHighPriority) CHANNEL_ALERT_ID else CHANNEL_PROTECTION_ID
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (isHighPriority) android.R.drawable.stat_sys_warning else android.R.drawable.ic_lock_idle_lock)
            .setPriority(if (isHighPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
