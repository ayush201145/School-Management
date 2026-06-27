package com.schoolmgmt.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.schoolmgmt.app.data.local.entity.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

/**
 * Stores the JWT and a few small bits of session info using
 * DataStore (not Room) — this is exactly what DataStore is for:
 * small, simple key-value app state, as opposed to the relational
 * data that lives in Room. Never store the password here, only the
 * token issued at login.
 */
@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("jwt_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val ROLE = stringPreferencesKey("role")
        val LAST_SYNC_SERVER_TIME = stringPreferencesKey("last_sync_server_time")
    }

    val tokenFlow: Flow<String?> = context.authDataStore.data.map { it[Keys.TOKEN] }
    val roleFlow: Flow<UserRole?> = context.authDataStore.data.map { prefs ->
        prefs[Keys.ROLE]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
    }

    suspend fun saveSession(token: String, userId: String, username: String, role: UserRole) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USERNAME] = username
            prefs[Keys.ROLE] = role.name
        }
    }

    suspend fun getToken(): String? = context.authDataStore.data.map { it[Keys.TOKEN] }.first()

    suspend fun getCurrentUserId(): String? = context.authDataStore.data.map { it[Keys.USER_ID] }.first()

    suspend fun clearSession() {
        context.authDataStore.edit { it.clear() }
    }

    /**
     * The sync engine's pull cursor — ALWAYS the server's clock
     * (serverTime from the last pull response), never the device's own
     * clock. Device clocks can drift or be wrong; using the device's
     * time here would risk silently missing updates. See SyncRepository.
     */
    suspend fun getLastSyncServerTime(): String? =
        context.authDataStore.data.map { it[Keys.LAST_SYNC_SERVER_TIME] }.first()

    suspend fun setLastSyncServerTime(serverTime: String) {
        context.authDataStore.edit { it[Keys.LAST_SYNC_SERVER_TIME] = serverTime }
    }
}
