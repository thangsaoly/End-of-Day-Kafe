package com.kafe.android.network

import android.content.Context
import android.content.SharedPreferences

class AuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kafe_auth_prefs", Context.MODE_PRIVATE)

    fun saveAuth(token: String, userId: Long, name: String, phoneNumber: String, email: String?, emailConfirmed: Boolean, role: String = "CUSTOMER") {
        prefs.edit().apply {
            putString("auth_token", token)
            putLong("user_id", userId)
            putString("user_name", name)
            putString("user_phone", phoneNumber)
            putString("user_email", email)
            putBoolean("email_confirmed", emailConfirmed)
            putString("user_role", role)
            apply()
        }
    }

    fun updateEmail(email: String, confirmed: Boolean) {
        prefs.edit().apply {
            putString("user_email", email)
            putBoolean("email_confirmed", confirmed)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString("auth_token", null)
    fun getUserId(): Long = prefs.getLong("user_id", -1L)
    fun getUserName(): String? = prefs.getString("user_name", "")
    fun getUserPhone(): String? = prefs.getString("user_phone", "")
    fun getUserEmail(): String? = prefs.getString("user_email", null)
    fun isEmailConfirmed(): Boolean = prefs.getBoolean("email_confirmed", false)
    fun getUserRole(): String = prefs.getString("user_role", "CUSTOMER") ?: "CUSTOMER"
    fun isMerchantRole(): Boolean = "MERCHANT".equals(getUserRole(), ignoreCase = true)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearAuth() {
        prefs.edit().clear().apply()
    }
}
