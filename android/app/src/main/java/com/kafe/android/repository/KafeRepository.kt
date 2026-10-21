package com.kafe.android.repository

import com.kafe.android.model.Cafe
import com.kafe.android.model.Reservation
import com.kafe.android.model.SurplusDeal
import com.kafe.android.network.*

// Requests are authenticated by FirebaseTokenInterceptor, so no token is passed here
class KafeRepository(private val api: KafeApi) {

    suspend fun getDeals(): Result<List<SurplusDeal>> = call { api.getDeals() }

    suspend fun reserveDeal(dealId: Long): Result<Reservation> = call { api.reserveDeal(dealId) }

    suspend fun getReservations(): Result<List<Reservation>> = call { api.getReservations() }

    suspend fun getMerchantReservations(): Result<List<Reservation>> = call { api.getMerchantReservations() }

    suspend fun claimReservation(reservationId: Long): Result<Reservation> =
        call { api.claimReservation(reservationId) }

    suspend fun claimReservationByCode(claimCode: String): Result<Reservation> =
        call { api.claimReservationByCode(claimCode) }

    suspend fun getFavorites(): Result<List<Cafe>> = call { api.getFavorites() }

    suspend fun addFavorite(cafeId: Long): Result<Unit> = call { api.addFavorite(cafeId); Unit }

    suspend fun removeFavorite(cafeId: Long): Result<Unit> = unitCall { api.removeFavorite(cafeId) }

    suspend fun registerDevice(pushToken: String): Result<Unit> =
        unitCall { api.registerDevice(DeviceTokenRequest(pushToken)) }

    suspend fun unregisterDevice(pushToken: String): Result<Unit> =
        unitCall { api.unregisterDevice(DeviceTokenRequest(pushToken)) }

    suspend fun startSession(name: String? = null, role: String? = null): Result<AuthResponse> =
        call { api.startSession(SessionRequest(name, role)) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    // For endpoints that return an empty body (204), where Retrofit doesn't throw on HTTP errors
    private suspend fun unitCall(block: suspend () -> retrofit2.Response<Unit>): Result<Unit> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(NetworkErrorParser.parseException(retrofit2.HttpException(response))))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }
}
