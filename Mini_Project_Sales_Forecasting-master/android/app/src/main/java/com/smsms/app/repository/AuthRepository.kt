package com.smsms.app.repository

import com.smsms.app.auth.AuthManager
import com.smsms.app.model.LoginRequest
import com.smsms.app.model.LoginResponse
import com.smsms.app.model.RegisterRequest
import com.smsms.app.model.UserSummary
import com.smsms.app.network.ApiClient
import com.smsms.app.network.safeApiCall
import com.smsms.app.util.ApiResult

object AuthRepository {

    private val api = ApiClient.api

    // On success, saves the token/user into AuthManager here — the one
    // place a login can actually happen — rather than leaving every
    // caller responsible for remembering to persist the session.
    suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        val result = safeApiCall { api.login(LoginRequest(username, password)) }
        if (result is ApiResult.Success) {
            AuthManager.login(result.data.token, result.data.user)
        }
        return result
    }

    suspend fun register(username: String, password: String, role: String): ApiResult<UserSummary> =
        safeApiCall { api.register(RegisterRequest(username, password, role)) }

    suspend fun logout() {
        AuthManager.logout()
    }
}
