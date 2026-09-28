package com.rakshacall.safety

import android.app.Application
import com.rakshacall.safety.core.notifications.NotificationHelper
import com.rakshacall.safety.di.ServiceLocator

class RakshaCallApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
        NotificationHelper.createNotificationChannels(this)
    }
}
