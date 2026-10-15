package com.kafe.android.repository

import com.kafe.android.model.Cafe
import com.kafe.android.model.Reservation
import com.kafe.android.model.SurplusDeal
import com.kafe.android.network.*

class KafeRepository(private val api: KafeApi) {

    suspend fun getDeals(): Result<List<SurplusDeal>> {
        return try {
            Result.success(api.getDeals())
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun reserveDeal(token: String, dealId: Long): Result<Reservation> {
        return try {
            Result.success(api.reserveDeal("Bearer $token", dealId))
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun getReservations(token: String): Result<List<Reservation>> {
        return try {
            Result.success(api.getReservations("Bearer $token"))
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun getMerchantReservations(token: String): Result<List<Reservation>> {
        return try {
            Result.success(api.getMerchantReservations("Bearer $token"))
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun claimReservation(token: String, reservationId: Long): Result<Reservation> {
        return try {
            Result.success(api.claimReservation("Bearer $token", reservationId))
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun claimReservationByCode(token: String, claimCode: String): Result<Reservation> {
        return try {
            Result.success(api.claimReservationByCode("Bearer $token", claimCode))
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun getFavorites(token: String): Result<List<Cafe>> {
        return try {
            Result.success(api.getFavorites("Bearer $token"))
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun addFavorite(token: String, cafeId: Long): Result<Unit> {
        return try {
            api.addFavorite("Bearer $token", cafeId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun removeFavorite(token: String, cafeId: Long): Result<Unit> {
        return unitCall { api.removeFavorite("Bearer $token", cafeId) }
    }

    suspend fun registerDevice(token: String, pushToken: String): Result<Unit> {
        return unitCall { api.registerDevice("Bearer $token", DeviceTokenRequest(pushToken)) }
    }

    suspend fun unregisterDevice(token: String, pushToken: String): Result<Unit> {
        return unitCall { api.unregisterDevice("Bearer $token", DeviceTokenRequest(pushToken)) }
    }

    // For endpoints that return an empty body (204), where Retrofit doesn't throw on HTTP errors
    private suspend fun unitCall(call: suspend () -> retrofit2.Response<Unit>): Result<Unit> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(NetworkErrorParser.parseException(retrofit2.HttpException(response))))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            Result.success(api.register(request))
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            Result.success(api.login(request))
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun linkEmail(token: String, email: String): Result<String> {
        return try {
            val response = api.linkEmail("Bearer $token", email)
            Result.success(response.string())
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun verifyEmail(token: String, request: VerifyEmailRequest): Result<String> {
        return try {
            val response = api.verifyEmail("Bearer $token", request)
            Result.success(response.string())
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun forgotPassword(request: ForgotPasswordRequest): Result<String> {
        return try {
            val response = api.forgotPassword(request)
            Result.success(response.string())
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }

    suspend fun resetPassword(request: ResetPasswordRequest): Result<String> {
        return try {
            val response = api.resetPassword(request)
            Result.success(response.string())
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorParser.parseException(e)))
        }
    }
}
