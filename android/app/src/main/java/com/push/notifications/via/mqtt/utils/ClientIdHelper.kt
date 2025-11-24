package com.push.notifications.via.mqtt.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

// Extension property to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mqtt_client_prefs")

/**
 * Helper class for generating and persisting a unique MQTT client ID using DataStore
 */
object ClientIdHelper {
    private val CLIENT_ID_KEY = stringPreferencesKey("client_id")
    private const val CLIENT_ID_PREFIX = "android_mqtt"

    /**
     * Gets or generates a unique client ID for this app instance
     * The ID is persisted in DataStore to remain consistent across app sessions
     */
    suspend fun getClientId(context: Context): String {
        // Try to read existing client ID
        val existingId = context.dataStore.data.map { preferences ->
            preferences[CLIENT_ID_KEY]
        }.first()

        if (existingId != null) {
            return existingId
        }

        // Generate a new client ID
        val newId = generateClientId()

        // Save it for future use
        context.dataStore.edit { preferences ->
            preferences[CLIENT_ID_KEY] = newId
        }

        return newId
    }

    /**
     * Generates a new unique client ID
     * Format: android_mqtt_<8-char-uuid>
     */
    private fun generateClientId(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "").take(8)
        return "${CLIENT_ID_PREFIX}_$uuid"
    }

    /**
     * Resets the client ID (generates a new one)
     * Useful for testing or if you want to create a new identity
     */
    suspend fun resetClientId(context: Context): String {
        val newId = generateClientId()
        context.dataStore.edit { preferences ->
            preferences[CLIENT_ID_KEY] = newId
        }
        return newId
    }
}
