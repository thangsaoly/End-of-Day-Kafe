package com.kafe.android.repository

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
