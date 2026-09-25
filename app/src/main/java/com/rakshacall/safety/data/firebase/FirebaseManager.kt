package com.rakshacall.safety.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.rakshacall.safety.core.security.SecurityLogger

/**
 * Production Firebase Manager configuring Authentication, Firestore, and App Check
 * for project `rakshacall`.
 */
object FirebaseManager {

    private const val PROJECT_ID = "rakshacall"
    private const val API_KEY = "AIzaSyCiJ5nB2kdxKbuHoXt2oydIqCcZYzdBbBs"
    private const val APP_ID = "1:779793034909:android:f95a56198e916578f66e74"
    private const val STORAGE_BUCKET = "rakshacall.firebasestorage.app"
    private const val GCM_SENDER_ID = "779793034909"

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(PROJECT_ID)
                    .setApiKey(API_KEY)
                    .setApplicationId(APP_ID)
                    .setStorageBucket(STORAGE_BUCKET)
                    .setGcmSenderId(GCM_SENDER_ID)
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.getInstance()
            }

            // Enable App Check with Play Integrity provider
            try {
                val appCheck = FirebaseAppCheck.getInstance(app)
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            } catch (e: Exception) {
                SecurityLogger.warn("Play Integrity App Check provider fallback: ${e.message}")
            }

            // Configure Firestore offline persistence
            try {
                val firestore = FirebaseFirestore.getInstance(app)
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
                    )
                    .build()
                firestore.firestoreSettings = settings
            } catch (e: Exception) {
                SecurityLogger.warn("Firestore settings configuration warning: ${e.message}")
            }

            isInitialized = true
            SecurityLogger.info("Firebase successfully initialized for project $PROJECT_ID")
        } catch (e: Exception) {
            SecurityLogger.error("Firebase initialization deferred (local-first mode active): ${e.message}")
        }
    }

    fun getAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }
}
