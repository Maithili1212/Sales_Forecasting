package com.smsms.app.auth

import okhttp3.Interceptor
import okhttp3.Response

// Attaches "Authorization: Bearer <token>" to every outgoing request that
// has one, so no individual API call needs to remember to add it itself.
// If the backend ever responds 401 (token invalid or expired), the local
// session is cleared — the next screen recomposition sees session == null
// and the nav graph sends the user back to the login screen, instead of
// every subsequent request just failing silently.
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = AuthManager.session.value?.token
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)
        if (response.code == 401) {
            AuthManager.logoutAsync()
        }
        return response
    }
}
