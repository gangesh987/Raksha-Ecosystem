package com.rakshacall.safety.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.rakshacall.safety.core.notifications.NotificationHelper

/**
 * Android Foreground Service maintaining persistent user awareness
 * and system notification while a protection session is in progress.
 */
class ProtectionForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.rakshacall.safety.service.START"
        const val ACTION_STOP = "com.rakshacall.safety.service.STOP"

        var isRunning = false
            private set
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NotificationHelper.ACTION_STOP_PROTECTION) {
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        val filter = IntentFilter(NotificationHelper.ACTION_STOP_PROTECTION)
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            stopReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            isRunning = false
            return START_NOT_STICKY
        }

        val notification = NotificationHelper.buildProtectionNotification(
            context = this,
            statusText = "RakshaCall protection is active."
        )

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_PROTECTION_ID,
                notification,
                foregroundServiceType
            )
            isRunning = true
        } catch (e: Exception) {
            // Fallback for missing permissions
            startForeground(NotificationHelper.NOTIFICATION_PROTECTION_ID, notification)
            isRunning = true
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            unregisterReceiver(stopReceiver)
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
