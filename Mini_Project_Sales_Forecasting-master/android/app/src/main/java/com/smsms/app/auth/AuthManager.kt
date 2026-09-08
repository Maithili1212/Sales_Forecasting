package com.smsms.app.auth

import android.content.Context
import com.smsms.app.model.UserSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// A single, app-wide source of truth for "who's logged in right now" —
// every screen that needs to know the current user or role reads this
// instead of each keeping its own copy that could drift out of sync.
// Backed by TokenStore for persistence (login survives the app process
// being killed), but reads/writes here go through an in-memory
// StateFlow so nothing needs to suspend just to check "is there a token".
object AuthManager {
    private lateinit var tokenStore: TokenStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    // False until the initial DataStore read completes. The navigation
    // graph waits for this before deciding whether to show the login
    // screen or go straight to the order list — otherwise every app
    // launch would flash the login screen for a split second even for
    // an already-logged-in user.
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun init(context: Context) {
        tokenStore = TokenStore(context.applicationContext)
        scope.launch {
            tokenStore.session.collect { session ->
                _session.value = session
                _isReady.value = true
            }
        }
    }

    suspend fun login(token: String, user: UserSummary) {
        tokenStore.save(token, user)
        // _session updates itself via the collect{} in init() once
        // DataStore's flow emits the write above.
    }

    suspend fun logout() {
        tokenStore.clear()
    }

    // Fire-and-forget logout for call sites that aren't suspend
    // functions themselves — namely AuthInterceptor, which runs on
    // OkHttp's own thread and reacts to a 401 by clearing the session.
    fun logoutAsync() {
        scope.launch { logout() }
    }

    fun currentRole(): String? = _session.value?.user?.role
}
