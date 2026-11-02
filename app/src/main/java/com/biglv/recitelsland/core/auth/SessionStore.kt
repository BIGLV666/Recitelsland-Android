package com.biglv.recitelsland.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.biglv.recitelsland.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.sessionDataStore by preferencesDataStore(name = "recite_session")

data class StoredSession(
    val token: String,
    val user: UserProfile,
)

class SessionStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read(): StoredSession? {
        val preferences = runCatching {
            context.sessionDataStore.data.first()
        }.getOrNull() ?: return null

        val token = preferences[KEY_TOKEN].orEmpty()
        val rawUser = preferences[KEY_USER].orEmpty()
        if (token.isBlank() || rawUser.isBlank()) return null
        val user = runCatching {
            json.decodeFromString(UserProfile.serializer(), rawUser)
        }.getOrNull() ?: return null
        return StoredSession(token, user)
    }

    suspend fun save(token: String, user: UserProfile) {
        context.sessionDataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
            preferences[KEY_USER] = json.encodeToString(UserProfile.serializer(), user)
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_USER = stringPreferencesKey("user")
    }
}
