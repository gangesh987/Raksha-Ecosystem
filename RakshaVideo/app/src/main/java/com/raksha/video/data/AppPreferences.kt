package com.raksha.video.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "raksha_video_preferences")

class AppPreferences(private val context: Context) {
    private val loggedInKey = booleanPreferencesKey("logged_in")
    private val userNameKey = stringPreferencesKey("user_name")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val onboardingKey = booleanPreferencesKey("permissions_onboarding_seen")
    private val darkThemeKey = booleanPreferencesKey("dark_theme")

    val loggedIn: Flow<Boolean> = context.dataStore.data.map { it[loggedInKey] ?: false }
    val userName: Flow<String> = context.dataStore.data.map { it[userNameKey] ?: "" }
    val userEmail: Flow<String> = context.dataStore.data.map { it[userEmailKey] ?: "" }
    val onboardingSeen: Flow<Boolean> = context.dataStore.data.map { it[onboardingKey] ?: false }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[darkThemeKey] ?: false }

    suspend fun login(name: String, email: String) {
        context.dataStore.edit {
            it[loggedInKey] = true
            it[userNameKey] = name
            it[userEmailKey] = email
        }
    }

    suspend fun logout() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun setOnboardingSeen() {
        context.dataStore.edit { it[onboardingKey] = true }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[darkThemeKey] = enabled }
    }
}
