package com.kafe.android.network

import android.content.Context
import android.content.SharedPreferences

// The signed-in user's backend profile. The Firebase session itself is kept by Firebase Auth.
class AuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kafe_auth_prefs", Context.MODE_PRIVATE)

    fun saveProfile(profile: AuthResponse) {
        prefs.edit().apply {
            clear() // also drops the old backend JWT from before Firebase Authentication
            putLong("user_id", profile.id)
            putString("user_name", profile.name)
            putString("user_email", profile.email)
            putBoolean("email_confirmed", profile.emailConfirmed)
            putString("user_role", profile.role)
            apply()
        }
    }

    fun getUserId(): Long = prefs.getLong("user_id", -1L)
    fun getUserName(): String? = prefs.getString("user_name", "")
    fun getUserEmail(): String? = prefs.getString("user_email", null)
    fun isEmailConfirmed(): Boolean = prefs.getBoolean("email_confirmed", false)
    fun getUserRole(): String = prefs.getString("user_role", "CUSTOMER") ?: "CUSTOMER"
    fun isMerchantRole(): Boolean = "MERCHANT".equals(getUserRole(), ignoreCase = true)

    fun isLoggedIn(): Boolean = getUserId() > 0

    fun clearAuth() {
        prefs.edit().clear().apply()
    }
}
