package com.kafe.android.network

import com.google.gson.Gson
import retrofit2.HttpException

sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Error(val message: String, val statusCode: Int? = null) : NetworkResult<Nothing>
}

object NetworkErrorParser {
    fun parseException(e: Throwable): String {
        return if (e is HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                try {
                    val gson = Gson()
                    val map = gson.fromJson(errorBody, Map::class.java)
                    val message = map["message"] as? String
                    if (!message.isNullOrBlank()) {
                        return message
                    }
                } catch (_: Exception) {
                    // Fallback to error body string if not json
                }
                errorBody
            } else {
                when (code) {
                    401 -> "Authentication failed. Please check your credentials or log in again."
                    403 -> "Account restricted or access forbidden."
                    404 -> "Item or endpoint not found."
                    409 -> "Deal is sold out or maximum active reservations reached."
                    else -> "Server error ($code). Please try again later."
                }
            }
        } else {
            e.localizedMessage ?: "Network error. Please check your internet connection."
        }
    }
}
