package com.kafe.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kafe.android.network.AuthManager
import com.kafe.android.network.KafeApi
import com.kafe.android.repository.KafeRepository
import com.kafe.android.ui.MainScreen
import com.kafe.android.viewmodel.KafeViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val api = KafeApi.create()
        val repository = KafeRepository(api)
        val authManager = AuthManager(applicationContext)
        val viewModel = ViewModelProvider(this, KafeViewModelFactory(repository, authManager))[KafeViewModel::class.java]

        setContent {
            MainScreen(viewModel = viewModel)
        }
    }
}

class KafeViewModelFactory(
    private val repository: KafeRepository,
    private val authManager: AuthManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KafeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KafeViewModel(repository, authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
