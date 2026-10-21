package com.kafe.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kafe.android.network.AuthManager
import com.kafe.android.network.KafeApi
import com.kafe.android.push.FirebasePushTokens
import com.kafe.android.push.Notifications
import com.kafe.android.push.PushTokens
import com.kafe.android.repository.KafeRepository
import com.kafe.android.ui.MainScreen
import com.kafe.android.viewmodel.KafeViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: KafeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Notifications.createChannels(this)

        val api = KafeApi.create()
        val repository = KafeRepository(api)
        val authManager = AuthManager(applicationContext)
        viewModel = ViewModelProvider(
            this,
            KafeViewModelFactory(repository, authManager, FirebasePushTokens())
        )[KafeViewModel::class.java]

        // Opened by tapping a notification while the app was closed
        if (savedInstanceState == null) {
            handleNotificationTap(intent)
        }

        setContent {
            MainScreen(viewModel = viewModel)
        }
    }

    // Opened by tapping a notification while the app was already running (singleTop)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationTap(intent)
    }

    private fun handleNotificationTap(intent: Intent?) {
        val dealId = intent?.getLongExtra(Notifications.EXTRA_DEAL_ID, -1L) ?: -1L
        if (dealId > 0) {
            viewModel.openDeal(dealId)
            intent?.removeExtra(Notifications.EXTRA_DEAL_ID)
        }
    }
}

class KafeViewModelFactory(
    private val repository: KafeRepository,
    private val authManager: AuthManager,
    private val pushTokens: PushTokens
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KafeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KafeViewModel(repository, authManager, pushTokens) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
