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
    // Firebase credentials are loaded from google-services.json by the Google Services plugin.
    // Do NOT hardcode API keys, app IDs, or sender IDs in source code.
    // If google-services.json is not present, Firebase will not initialize and the app
    // will operate in local-only mode.

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            // Standard initialization: reads from google-services.json via the Gradle plugin.
            // If the plugin hasn't processed the config, fall back gracefully.
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                    ?: throw IllegalStateException("google-services.json not found or Firebase plugin not applied")
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
