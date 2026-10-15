package com.kafe.android.network

import android.os.Build
import com.kafe.android.BuildConfig
import com.kafe.android.model.Cafe
import com.kafe.android.model.Reservation
import com.kafe.android.model.SurplusDeal
import com.kafe.android.auth.AuthClient
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Auth DTOs. Sign-in itself happens in Firebase; the backend only creates the profile.
data class SessionRequest(
    val name: String? = null,
    val role: String? = null
)

data class AuthResponse(
    val id: Long,
    val name: String,
    val phoneNumber: String?,
    val email: String?,
    val emailConfirmed: Boolean,
    val role: String = "CUSTOMER",
    val cafeId: Long? = null
)

data class DeviceTokenRequest(
    val token: String
)

interface KafeApi {

    @GET("deals")
    suspend fun getDeals(): List<SurplusDeal>

    @POST("deals/{id}/reserve")
    suspend fun reserveDeal(
        @Path("id") id: Long
    ): Reservation

    @GET("reservations")
    suspend fun getReservations(): List<Reservation>

    // Merchant Endpoints
    @GET("merchant/reservations")
    suspend fun getMerchantReservations(): List<Reservation>

    @POST("merchant/reservations/{id}/claim")
    suspend fun claimReservation(
        @Path("id") id: Long
    ): Reservation

    @POST("merchant/reservations/claim-by-code/{claimCode}")
    suspend fun claimReservationByCode(
        @Path("claimCode") claimCode: String
    ): Reservation

    // Favourite cafes
    @GET("favorites")
    suspend fun getFavorites(): List<Cafe>

    @POST("favorites/{cafeId}")
    suspend fun addFavorite(
        @Path("cafeId") cafeId: Long
    ): Cafe

    @DELETE("favorites/{cafeId}")
    suspend fun removeFavorite(
        @Path("cafeId") cafeId: Long
    ): Response<Unit>

    // Push notification device tokens
    @POST("devices")
    suspend fun registerDevice(
        @Body request: DeviceTokenRequest
    ): Response<Unit>

    @POST("devices/unregister")
    suspend fun unregisterDevice(
        @Body request: DeviceTokenRequest
    ): Response<Unit>

    // Called after every Firebase sign-in; creates the backend account the first time
    @POST("auth/session")
    suspend fun startSession(
        @Body request: SessionRequest
    ): AuthResponse

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

        fun create(authClient: AuthClient, baseUrl: String = getBaseUrl()): KafeApi {
            android.util.Log.d("KafeApi", "Connecting to Base URL: $baseUrl")
            val client = OkHttpClient.Builder()
                .addInterceptor(FirebaseTokenInterceptor(authClient))
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(KafeApi::class.java)
        }
    }
}

// Adds "Authorization: Bearer <Firebase ID token>" when signed in. Runs on OkHttp's
// background threads, so blocking for the (usually cached) token is fine.
class FirebaseTokenInterceptor(private val authClient: AuthClient) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = runCatching { runBlocking { authClient.idToken() } }.getOrNull()
        val request = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
