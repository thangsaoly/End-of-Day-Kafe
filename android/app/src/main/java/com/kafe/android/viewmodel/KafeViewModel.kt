package com.kafe.android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kafe.android.auth.AuthClient
import com.kafe.android.model.Reservation
import com.kafe.android.model.SurplusDeal
import com.kafe.android.network.*
import com.kafe.android.push.PushTokens
import com.kafe.android.repository.KafeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    object Loading : UiState
    data class Success(val deals: List<SurplusDeal>) : UiState
    data class Error(val message: String) : UiState
}

sealed interface UiEvent {
    data class ReservationSuccess(val reservation: Reservation) : UiEvent
    data class ReservationError(val message: String) : UiEvent
    data class ClaimSuccess(val reservation: Reservation) : UiEvent
    data class ClaimError(val message: String) : UiEvent
    data class FavoriteError(val message: String) : UiEvent
}

class KafeViewModel(
    private val repository: KafeRepository,
    private val authManager: AuthManager,
    private val authClient: AuthClient,
    private val pushTokens: PushTokens = PushTokens.None
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _reservationsState = MutableStateFlow<List<Reservation>>(emptyList())
    val reservationsState: StateFlow<List<Reservation>> = _reservationsState.asStateFlow()

    private val _merchantReservationsState = MutableStateFlow<List<Reservation>>(emptyList())
    val merchantReservationsState: StateFlow<List<Reservation>> = _merchantReservationsState.asStateFlow()

    private val _favoriteCafeIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteCafeIds: StateFlow<Set<Long>> = _favoriteCafeIds.asStateFlow()

    // Deal to scroll to after tapping a "new deal" notification; cleared once shown
    private val _focusedDealId = MutableStateFlow<Long?>(null)
    val focusedDealId: StateFlow<Long?> = _focusedDealId.asStateFlow()

    var isLoggedIn: Boolean by mutableStateOf(false)
        private set

    var isRefreshingReservations: Boolean by mutableStateOf(false)
        private set

    var isRefreshingMerchantReservations: Boolean by mutableStateOf(false)
        private set

    var isMerchantMode: Boolean by mutableStateOf(false)

    var userRole: String by mutableStateOf("CUSTOMER")
        private set

    var userEmail: String? by mutableStateOf(null)
        private set

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    init {
        // A saved profile without a Firebase session (e.g. from the old phone login) is stale
        if (authManager.isLoggedIn() && !authClient.isSignedIn) {
            authManager.clearAuth()
        }
        loadDeals()
        if (authManager.isLoggedIn()) {
            onSignedIn()
        }
    }

    private fun onSignedIn() {
        isLoggedIn = true
        userRole = authManager.getUserRole()
        userEmail = authManager.getUserEmail()
        isMerchantMode = authManager.isMerchantRole()
        if (isMerchantMode) {
            loadMerchantReservations()
        } else {
            loadReservations()
        }
        loadFavorites()
        registerPushToken()
    }

    fun toggleMerchantMode() {
        isMerchantMode = !isMerchantMode
        if (isMerchantMode && isLoggedIn) {
            loadMerchantReservations()
        }
    }

    fun loadDeals() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.getDeals()
                .onSuccess { deals ->
                    _uiState.value = UiState.Success(deals.sortedBy { it.quantityAvailable == 0 })
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to fetch deals")
                }
        }
    }

    fun loadReservations() {
        if (!isLoggedIn) return
        isRefreshingReservations = true
        viewModelScope.launch {
            repository.getReservations()
                .onSuccess { list ->
                    _reservationsState.value = list
                    isRefreshingReservations = false
                }
                .onFailure {
                    isRefreshingReservations = false
                }
        }
    }

    fun loadMerchantReservations() {
        if (!isLoggedIn) return
        isRefreshingMerchantReservations = true
        viewModelScope.launch {
            repository.getMerchantReservations()
                .onSuccess { list ->
                    _merchantReservationsState.value = list
                    isRefreshingMerchantReservations = false
                }
                .onFailure {
                    isRefreshingMerchantReservations = false
                }
        }
    }

    fun claimReservation(reservationId: Long) {
        if (!isLoggedIn) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ClaimError("Please log in as shop owner."))
            }
            return
        }

        viewModelScope.launch {
            repository.claimReservation(reservationId)
                .onSuccess { updated ->
                    loadMerchantReservations()
                    loadReservations()
                    _eventFlow.emit(UiEvent.ClaimSuccess(updated))
                }
                .onFailure { error ->
                    _eventFlow.emit(UiEvent.ClaimError(error.message ?: "Failed to claim reservation."))
                }
        }
    }

    fun claimReservationByCode(claimCode: String) {
        if (!isLoggedIn) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ClaimError("Please log in as shop owner."))
            }
            return
        }

        val cleanCode = claimCode.trim()
        if (cleanCode.isEmpty()) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ClaimError("Please enter a valid claim code."))
            }
            return
        }

        viewModelScope.launch {
            repository.claimReservationByCode(cleanCode)
                .onSuccess { updated ->
                    loadMerchantReservations()
                    loadReservations()
                    _eventFlow.emit(UiEvent.ClaimSuccess(updated))
                }
                .onFailure { error ->
                    _eventFlow.emit(UiEvent.ClaimError(error.message ?: "Invalid claim code or already claimed."))
                }
        }
    }

    fun reserveDeal(dealId: Long) {
        if (!isLoggedIn) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ReservationError("Please log in to make a reservation."))
            }
            return
        }

        viewModelScope.launch {
            repository.reserveDeal(dealId)
                .onSuccess { reservation ->
                    // Update local deal quantity in _uiState
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        val updatedDeals = currentState.deals.map { deal ->
                            if (deal.id == reservation.deal.id) {
                                deal.copy(quantityAvailable = reservation.deal.quantityAvailable)
                            } else {
                                deal
                            }
                        }.sortedBy { it.quantityAvailable == 0 }
                        _uiState.value = UiState.Success(updatedDeals)
                    }
                    loadReservations() // reload reservations feed
                    _eventFlow.emit(UiEvent.ReservationSuccess(reservation))
                }
                .onFailure { error ->
                    val errorMsg = error.message ?: "Failed to reserve item."
                    _eventFlow.emit(UiEvent.ReservationError(errorMsg))
                }
        }
    }

    // Authentication: Firebase signs the user in, then the backend creates/loads their profile

    fun login(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        authenticate(onResult) {
            authClient.signInWithEmail(email, password)
            SessionRequest()
        }
    }

    fun register(name: String, email: String, password: String, role: String = "CUSTOMER", onResult: (Result<Unit>) -> Unit) {
        authenticate(onResult) {
            authClient.signUpWithEmail(email, password, name)
            SessionRequest(name = name, role = role)
        }
    }

    // The role only matters if this Google account is new to the backend
    fun signInWithGoogle(googleIdToken: String, role: String = "CUSTOMER", onResult: (Result<Unit>) -> Unit) {
        authenticate(onResult) {
            authClient.signInWithGoogle(googleIdToken)
            SessionRequest(role = role)
        }
    }

    private fun authenticate(onResult: (Result<Unit>) -> Unit, firebaseSignIn: suspend () -> SessionRequest) {
        viewModelScope.launch {
            val session = try {
                firebaseSignIn()
            } catch (e: Exception) {
                onResult(Result.failure(e))
                return@launch
            }
            repository.startSession(session.name, session.role)
                .onSuccess { profile ->
                    authManager.saveProfile(profile)
                    onSignedIn()
                    loadDeals()
                    onResult(Result.success(Unit))
                }
                .onFailure { error ->
                    // e.g. banned or backend unreachable: don't stay half signed in
                    authClient.signOut()
                    onResult(Result.failure(error))
                }
        }
    }

    fun forgotPassword(email: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            try {
                authClient.sendPasswordReset(email)
                // Same message whether or not the account exists, so emails can't be probed
                onResult(Result.success("If an account exists for $email, a password reset link has been sent."))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun logout() {
        val wasLoggedIn = isLoggedIn
        _favoriteCafeIds.value = emptySet()
        authManager.clearAuth()
        isLoggedIn = false
        userRole = "CUSTOMER"
        userEmail = null
        isMerchantMode = false
        _reservationsState.value = emptyList()
        _merchantReservationsState.value = emptyList()
        loadDeals()

        viewModelScope.launch {
            // Stop this phone getting the user's alerts. Must run before signOut,
            // while the request can still carry the user's Firebase token.
            if (wasLoggedIn) {
                pushTokens.current()?.let { repository.unregisterDevice(it) }
            }
            authClient.signOut()
        }
    }

    fun loadFavorites() {
        if (!isLoggedIn) return
        viewModelScope.launch {
            repository.getFavorites()
                .onSuccess { cafes -> _favoriteCafeIds.value = cafes.map { it.id }.toSet() }
        }
    }

    // Updates the heart immediately, then reverts if the server call fails
    fun toggleFavorite(cafeId: Long) {
        if (!isLoggedIn) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.FavoriteError("Please log in to follow cafes."))
            }
            return
        }

        val wasFavorite = cafeId in _favoriteCafeIds.value
        _favoriteCafeIds.value = if (wasFavorite) _favoriteCafeIds.value - cafeId else _favoriteCafeIds.value + cafeId

        viewModelScope.launch {
            val result = if (wasFavorite) {
                repository.removeFavorite(cafeId)
            } else {
                repository.addFavorite(cafeId)
            }
            result.onFailure { error ->
                _favoriteCafeIds.value = if (wasFavorite) _favoriteCafeIds.value + cafeId else _favoriteCafeIds.value - cafeId
                _eventFlow.emit(UiEvent.FavoriteError(error.message ?: "Could not update favourites."))
            }
        }
    }

    // Reload so a just-posted deal is in the list, then let the Deals tab scroll to it
    fun openDeal(dealId: Long) {
        _focusedDealId.value = dealId
        loadDeals()
    }

    fun onDealFocused() {
        _focusedDealId.value = null
    }

    private fun registerPushToken() {
        viewModelScope.launch {
            val pushToken = pushTokens.current() ?: return@launch
            repository.registerDevice(pushToken)
        }
    }
}
