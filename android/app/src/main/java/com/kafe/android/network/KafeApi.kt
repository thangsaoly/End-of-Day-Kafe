package com.kafe.android.network

import android.os.Build
import com.kafe.android.BuildConfig
import com.kafe.android.model.Reservation
import com.kafe.android.model.SurplusDeal
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Auth DTOs
data class RegisterRequest(
    val name: String,
    val phoneNumber: String,
    val password: String,
    val role: String = "CUSTOMER"
)

data class LoginRequest(
    val phoneNumber: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val email: String?,
    val emailConfirmed: Boolean,
    val role: String = "CUSTOMER"
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

interface KafeApi {

    @GET("deals")
    suspend fun getDeals(): List<SurplusDeal>

    @POST("deals/{id}/reserve")
    suspend fun reserveDeal(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Reservation

    @GET("reservations")
    suspend fun getReservations(
        @Header("Authorization") token: String
    ): List<Reservation>

    // Merchant Endpoints
    @GET("merchant/reservations")
    suspend fun getMerchantReservations(
        @Header("Authorization") token: String
    ): List<Reservation>

    @POST("merchant/reservations/{id}/claim")
    suspend fun claimReservation(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Reservation

    @POST("merchant/reservations/claim-by-code/{claimCode}")
    suspend fun claimReservationByCode(
        @Header("Authorization") token: String,
        @Path("claimCode") claimCode: String
    ): Reservation

    // Authentication Endpoints
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): AuthResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("auth/link-email")
    suspend fun linkEmail(
        @Header("Authorization") token: String,
        @Query("email") email: String
    ): ResponseBody

    @POST("auth/verify-email")
    suspend fun verifyEmail(
        @Header("Authorization") token: String,
        @Body request: VerifyEmailRequest
    ): ResponseBody

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): ResponseBody

    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): ResponseBody

    companion object {
        private fun getBaseUrl(): String {
            if (!BuildConfig.DEBUG) {
                // Production API URL for release build/customers
                return "https://api.kafe-rescue.com/api/"
            }

            // Check if running on an emulator
            val isEmulator = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.HARDWARE.contains("goldfish")
                    || Build.HARDWARE.contains("ranchu")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.PRODUCT.contains("sdk_google")
                    || Build.PRODUCT.contains("google_sdk")
                    || Build.PRODUCT.contains("sdk")
                    || Build.PRODUCT.contains("sdk_x86")
                    || Build.PRODUCT.contains("vbox86p")
                    || Build.PRODUCT.contains("emulator")
                    || Build.PRODUCT.contains("simulator")

            return if (isEmulator) {
                if (Build.MANUFACTURER.contains("Genymotion")) {
                    "http://10.0.3.2:8082/api/"
                } else {
                    "http://10.0.2.2:8082/api/"
                }
            } else {
                // Real device in debug mode: Points to your laptop's current local network IP
                "http://10.10.10.77:8082/api/"
            }
        }

        fun create(baseUrl: String = getBaseUrl()): KafeApi {
            android.util.Log.d("KafeApi", "Connecting to Base URL: $baseUrl")
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(KafeApi::class.java)
        }
    }
}
