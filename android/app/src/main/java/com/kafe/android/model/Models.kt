package com.kafe.android.model

import com.google.gson.annotations.SerializedName

data class Cafe(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val closingTime: String,
    val imageUrl: String?
)

data class SurplusDeal(
    val id: Long,
    val cafe: Cafe,
    val title: String,
    val originalPrice: Double,
    val discountPrice: Double,
    val quantityAvailable: Int,
    val pickupStart: String,
    val pickupEnd: String,
    val imageUrl: String?
)

data class User(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val email: String?,
    val emailConfirmed: Boolean,
    val noShowCount: Int,
    // The backend serializes the Java boolean getter isBanned() as "banned"
    @SerializedName("banned")
    val isBanned: Boolean
)

data class Reservation(
    val id: Long,
    val deal: SurplusDeal,
    val user: User,
    val claimCode: String,
    val status: String,
    val createdAt: String
)
