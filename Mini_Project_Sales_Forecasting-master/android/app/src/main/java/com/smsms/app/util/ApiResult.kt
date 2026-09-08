package com.smsms.app.util

// A tiny "either success or failure with a message" wrapper. The repository
// layer converts every network call into one of these, so every screen
// handles success/failure the same way instead of each one re-implementing
// try/catch + HTTP-status checks.
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()
}
