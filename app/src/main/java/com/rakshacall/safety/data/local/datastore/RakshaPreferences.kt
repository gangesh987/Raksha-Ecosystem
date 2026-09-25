package com.rakshacall.safety.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.rakshaDataStore: DataStore<Preferences> by preferencesDataStore(name = "rakshacall_preferences")

/**
 * Encapsulates DataStore preferences for session authentication,
 * risk thresholds, privacy settings, and app options.
 */
class RakshaPreferences(private val context: Context) {

    companion object {
        // Authentication & Session
        val KEY_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_RAKSHA_ID = stringPreferencesKey("raksha_id")
        val KEY_PHONE_NUMBER = stringPreferencesKey("phone_number")
        val KEY_AUTH_TIMESTAMP = longPreferencesKey("auth_timestamp")

        // Risk Engine Thresholds
        val KEY_LOW_THRESHOLD = intPreferencesKey("low_threshold")
        val KEY_HIGH_THRESHOLD = intPreferencesKey("high_threshold")
        val KEY_CRITICAL_THRESHOLD = intPreferencesKey("critical_threshold")

        // Privacy & Sensors
        val KEY_VISUAL_ANALYSIS_ENABLED = booleanPreferencesKey("visual_analysis_enabled")
        val KEY_LIVENESS_ENABLED = booleanPreferencesKey("liveness_enabled")
        val KEY_RETENTION_DAYS = intPreferencesKey("retention_days")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

        // Permission tracking
        val KEY_MIC_PERMISSION_REQUESTED = booleanPreferencesKey("mic_requested")
        val KEY_CAM_PERMISSION_REQUESTED = booleanPreferencesKey("cam_requested")
    }

    // Read flows
    val isLoggedIn: Flow<Boolean> = context.rakshaDataStore.data.map { it[KEY_LOGGED_IN] ?: false }
    val rakshaCallId: Flow<String> = context.rakshaDataStore.data.map { it[KEY_RAKSHA_ID] ?: "" }
    val userId: Flow<String> = context.rakshaDataStore.data.map { it[KEY_USER_ID] ?: "" }
    val phoneNumber: Flow<String> = context.rakshaDataStore.data.map { it[KEY_PHONE_NUMBER] ?: "" }

    val lowThreshold: Flow<Int> = context.rakshaDataStore.data.map { it[KEY_LOW_THRESHOLD] ?: 30 }
    val highThreshold: Flow<Int> = context.rakshaDataStore.data.map { it[KEY_HIGH_THRESHOLD] ?: 60 }
    val criticalThreshold: Flow<Int> = context.rakshaDataStore.data.map { it[KEY_CRITICAL_THRESHOLD] ?: 80 }

    val visualAnalysisEnabled: Flow<Boolean> = context.rakshaDataStore.data.map { it[KEY_VISUAL_ANALYSIS_ENABLED] ?: true }
    val livenessEnabled: Flow<Boolean> = context.rakshaDataStore.data.map { it[KEY_LIVENESS_ENABLED] ?: false }
    val retentionDays: Flow<Int> = context.rakshaDataStore.data.map { it[KEY_RETENTION_DAYS] ?: 30 }
    val notificationsEnabled: Flow<Boolean> = context.rakshaDataStore.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }

    // Update methods
    suspend fun saveAuthSession(id: String, rakshaId: String, phone: String) {
        context.rakshaDataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_USER_ID] = id
            prefs[KEY_RAKSHA_ID] = rakshaId
            prefs[KEY_PHONE_NUMBER] = phone
            prefs[KEY_AUTH_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun clearAuthSession() {
        context.rakshaDataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = false
            prefs[KEY_USER_ID] = ""
            prefs[KEY_RAKSHA_ID] = ""
            prefs[KEY_PHONE_NUMBER] = ""
        }
    }

    suspend fun updateThresholds(low: Int, high: Int, critical: Int) {
        context.rakshaDataStore.edit { prefs ->
            prefs[KEY_LOW_THRESHOLD] = low
            prefs[KEY_HIGH_THRESHOLD] = high
            prefs[KEY_CRITICAL_THRESHOLD] = critical
        }
    }

    suspend fun setVisualAnalysisEnabled(enabled: Boolean) {
        context.rakshaDataStore.edit { it[KEY_VISUAL_ANALYSIS_ENABLED] = enabled }
    }

    suspend fun setLivenessEnabled(enabled: Boolean) {
        context.rakshaDataStore.edit { it[KEY_LIVENESS_ENABLED] = enabled }
    }

    suspend fun setRetentionDays(days: Int) {
        context.rakshaDataStore.edit { it[KEY_RETENTION_DAYS] = days }
    }

    suspend fun clearAllPreferences() {
        context.rakshaDataStore.edit { it.clear() }
    }
}
