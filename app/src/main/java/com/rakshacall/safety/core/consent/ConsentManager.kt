package com.rakshacall.safety.core.consent

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.rakshacall.safety.domain.model.ConsentRecord
import com.rakshacall.safety.domain.model.ConsentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.consentDataStore by preferencesDataStore(name = "rakshacall_consent")

/**
 * Formal Consent Manager tracking granular user consent with policy versioning.
 * Strictly adheres to privacy-by-design principles.
 */
class ConsentManager(private val context: Context) {

    companion object {
        const val CURRENT_POLICY_VERSION = "2.0.0"
    }

    private fun keyFor(type: ConsentType) = booleanPreferencesKey("consent_${type.name}")

    fun observeConsent(type: ConsentType): Flow<Boolean> {
        return context.consentDataStore.data.map { preferences ->
            preferences[keyFor(type)] ?: false
        }
    }

    suspend fun grantConsent(type: ConsentType): ConsentRecord {
        context.consentDataStore.edit { preferences ->
            preferences[keyFor(type)] = true
        }
        return ConsentRecord(
            id = UUID.randomUUID().toString(),
            type = type,
            grantedAt = System.currentTimeMillis(),
            policyVersion = CURRENT_POLICY_VERSION
        )
    }

    suspend fun revokeConsent(type: ConsentType): ConsentRecord {
        context.consentDataStore.edit { preferences ->
            preferences[keyFor(type)] = false
        }
        return ConsentRecord(
            id = UUID.randomUUID().toString(),
            type = type,
            grantedAt = 0L,
            revokedAt = System.currentTimeMillis(),
            policyVersion = CURRENT_POLICY_VERSION
        )
    }

    suspend fun resetAllConsents() {
        context.consentDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
