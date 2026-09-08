package com.smsms.app.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smsms.app.model.UserSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore is the modern replacement for SharedPreferences — same idea
// (small key/value pairs saved to disk), but backed by Kotlin Flows
// instead of synchronous reads, so it can't ever block the UI thread.
private val Context.authDataStore by preferencesDataStore(name = "auth")

private val TOKEN_KEY = stringPreferencesKey("token")
private val USER_ID_KEY = stringPreferencesKey("user_id")
private val USERNAME_KEY = stringPreferencesKey("username")
private val ROLE_KEY = stringPreferencesKey("role")

// Wraps the raw DataStore calls so nothing else in the app needs to know
// the actual preference key names or how to build a UserSummary back out
// of four separate string values.
class TokenStore(private val context: Context) {

    val session: Flow<Session?> = context.authDataStore.data.map { prefs ->
        val token = prefs[TOKEN_KEY] ?: return@map null
        val userId = prefs[USER_ID_KEY]?.toIntOrNull() ?: return@map null
        val username = prefs[USERNAME_KEY] ?: return@map null
        val role = prefs[ROLE_KEY] ?: return@map null
        Session(token, UserSummary(userId, username, role))
    }

    suspend fun currentToken(): String? = session.first()?.token

    suspend fun save(token: String, user: UserSummary) {
        context.authDataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = user.user_id.toString()
            prefs[USERNAME_KEY] = user.username
            prefs[ROLE_KEY] = user.role
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }
}

data class Session(val token: String, val user: UserSummary)
