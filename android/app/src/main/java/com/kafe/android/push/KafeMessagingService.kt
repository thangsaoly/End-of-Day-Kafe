package com.kafe.android.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kafe.android.network.AuthManager
import com.kafe.android.network.KafeApi
import com.kafe.android.repository.KafeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class KafeMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // The backend sends data-only messages, so this runs in the foreground and background alike
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "NEW_DEAL") return
        val dealId = data["dealId"]?.toLongOrNull() ?: return
        Notifications.showNewDeal(
            context = this,
            dealId = dealId,
            title = data["title"] ?: "New deal nearby",
            body = data["body"] ?: "A cafe you follow just posted a surplus deal."
        )
    }

    // FCM rotated this install's token; tell the backend if someone is logged in
    override fun onNewToken(token: String) {
        val authToken = AuthManager(applicationContext).getToken() ?: return
        scope.launch {
            KafeRepository(KafeApi.create()).registerDevice(authToken, token)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
