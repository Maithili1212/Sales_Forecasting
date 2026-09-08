package com.smsms.app.model

data class LoginRequest(val username: String, val password: String)

data class UserSummary(val user_id: Int, val username: String, val role: String)

data class LoginResponse(val token: String, val user: UserSummary)

data class RegisterRequest(val username: String, val password: String, val role: String)
