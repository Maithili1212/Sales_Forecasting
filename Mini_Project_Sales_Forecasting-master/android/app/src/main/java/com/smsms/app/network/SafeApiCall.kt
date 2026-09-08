package com.smsms.app.network

import com.google.gson.Gson
import com.smsms.app.model.ApiErrorResponse
import com.smsms.app.util.ApiResult
import java.io.IOException
import retrofit2.Response

private val gson = Gson()

private fun <T> toResult(response: Response<T>): ApiResult<T> {
    if (response.isSuccessful) {
        val body = response.body()
        return if (body != null) ApiResult.Success(body) else ApiResult.Failure("Empty response from server.")
    }
    val message = try {
        val errorJson = response.errorBody()?.string()
        gson.fromJson(errorJson, ApiErrorResponse::class.java)?.error
    } catch (e: Exception) {
        null
    } ?: "Request failed (HTTP ${response.code()})."
    return ApiResult.Failure(message)
}

// Shared by every repository: turns a Retrofit Response — which might be
// a 2xx with a body, OR a 4xx/5xx carrying an `{ "error": "..." }` JSON
// body, OR a thrown exception if the device can't reach the server at
// all — into a single ApiResult every screen can handle the same way.
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> =
    try {
        toResult(call())
    } catch (e: IOException) {
        ApiResult.Failure("Could not reach the server. Is the backend running and reachable? (${e.message})")
    }
