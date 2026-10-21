package com.kafe.android.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Source of this install's push token. An interface so the ViewModel can be tested without Firebase. */
interface PushTokens {
    suspend fun current(): String?

    object None : PushTokens {
        override suspend fun current(): String? = null
    }
}

class FirebasePushTokens : PushTokens {
    // Returns null instead of throwing (e.g. no Google Play services, or placeholder google-services.json)
    override suspend fun current(): String? = try {
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    } catch (e: Exception) {
        null
    }
}
