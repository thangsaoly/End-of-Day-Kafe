package com.kafe.android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    var isLoggedIn: Boolean by mutableStateOf(authManager.isLoggedIn())
        private set

    var isRefreshingReservations: Boolean by mutableStateOf(false)
        private set

    var isRefreshingMerchantReservations: Boolean by mutableStateOf(false)
        private set

    var isMerchantMode: Boolean by mutableStateOf(authManager.isMerchantRole())

    var userRole: String by mutableStateOf(authManager.getUserRole())
        private set

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    init {
        loadDeals()
        if (isLoggedIn) {
            userRole = authManager.getUserRole()
            isMerchantMode = authManager.isMerchantRole()
            if (isMerchantMode) {
                loadMerchantReservations()
            } else {
                loadReservations()
            }
            loadFavorites()
            registerPushToken()
        }
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
        val token = authManager.getToken() ?: return
        isRefreshingReservations = true
        viewModelScope.launch {
            repository.getReservations(token)
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
        val token = authManager.getToken() ?: return
        isRefreshingMerchantReservations = true
        viewModelScope.launch {
            repository.getMerchantReservations(token)
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
        val token = authManager.getToken()
        if (token == null) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ClaimError("Please log in as shop owner."))
            }
            return
        }

        viewModelScope.launch {
            repository.claimReservation(token, reservationId)
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
        val token = authManager.getToken()
        if (token == null) {
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
            repository.claimReservationByCode(token, cleanCode)
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
        val token = authManager.getToken()
        if (token == null) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ReservationError("Please log in to make a reservation."))
            }
            return
        }

        viewModelScope.launch {
            repository.reserveDeal(token, dealId)
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

    // Authentication Actions
    fun login(phone: String, pass: String, onResult: (Result<AuthResponse>) -> Unit) {
        viewModelScope.launch {
            repository.login(LoginRequest(phone, pass))
                .onSuccess { response ->
                    authManager.saveAuth(
                        token = response.token,
                        userId = response.id,
                        name = response.name,
                        phoneNumber = response.phoneNumber,
                        email = response.email,
                        emailConfirmed = response.emailConfirmed,
                        role = response.role
                    )
                    isLoggedIn = true
                    userRole = response.role
                    isMerchantMode = "MERCHANT".equals(response.role, ignoreCase = true)
                    loadDeals()
                    if (isMerchantMode) {
                        loadMerchantReservations()
                    } else {
                        loadReservations()
                    }
                    loadFavorites()
                    registerPushToken()
                    onResult(Result.success(response))
                }
                .onFailure {
                    onResult(Result.failure(it))
                }
        }
    }

    fun register(name: String, phone: String, pass: String, role: String = "CUSTOMER", onResult: (Result<AuthResponse>) -> Unit) {
        viewModelScope.launch {
            repository.register(RegisterRequest(name, phone, pass, role))
                .onSuccess { response ->
                    authManager.saveAuth(
                        token = response.token,
                        userId = response.id,
                        name = response.name,
                        phoneNumber = response.phoneNumber,
                        email = response.email,
                        emailConfirmed = response.emailConfirmed,
                        role = response.role
                    )
                    isLoggedIn = true
                    userRole = response.role
                    isMerchantMode = "MERCHANT".equals(response.role, ignoreCase = true)
                    loadDeals()
                    if (isMerchantMode) {
                        loadMerchantReservations()
                    } else {
                        loadReservations()
                    }
                    loadFavorites()
                    registerPushToken()
                    onResult(Result.success(response))
                }
                .onFailure {
                    onResult(Result.failure(it))
                }
        }
    }

    fun linkEmail(email: String, onResult: (Result<String>) -> Unit) {
        val token = authManager.getToken() ?: return
        viewModelScope.launch {
            repository.linkEmail(token, email)
                .onSuccess { msg ->
                    authManager.updateEmail(email, false)
                    onResult(Result.success(msg))
                }
                .onFailure {
                    onResult(Result.failure(it))
                }
        }
    }

    fun verifyEmail(email: String, code: String, onResult: (Result<String>) -> Unit) {
        val token = authManager.getToken() ?: return
        viewModelScope.launch {
            repository.verifyEmail(token, VerifyEmailRequest(email, code))
                .onSuccess { msg ->
                    authManager.updateEmail(email, true)
                    onResult(Result.success(msg))
                }
                .onFailure {
                    onResult(Result.failure(it))
                }
        }
    }

    fun forgotPassword(email: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            repository.forgotPassword(ForgotPasswordRequest(email))
                .onSuccess { onResult(Result.success(it)) }
                .onFailure { onResult(Result.failure(it)) }
        }
    }

    fun resetPassword(email: String, code: String, pass: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            repository.resetPassword(ResetPasswordRequest(email, code, pass))
                .onSuccess { onResult(Result.success(it)) }
                .onFailure { onResult(Result.failure(it)) }
        }
    }

    fun logout() {
        // Stop this phone getting the user's alerts. Grab the auth token before it is cleared.
        authManager.getToken()?.let { authToken ->
            viewModelScope.launch {
                pushTokens.current()?.let { repository.unregisterDevice(authToken, it) }
            }
        }
        _favoriteCafeIds.value = emptySet()
        authManager.clearAuth()
        isLoggedIn = false
        userRole = "CUSTOMER"
        isMerchantMode = false
        _reservationsState.value = emptyList()
        _merchantReservationsState.value = emptyList()
        loadDeals()
    }

    fun loadFavorites() {
        val token = authManager.getToken() ?: return
        viewModelScope.launch {
            repository.getFavorites(token)
                .onSuccess { cafes -> _favoriteCafeIds.value = cafes.map { it.id }.toSet() }
        }
    }

    // Updates the heart immediately, then reverts if the server call fails
    fun toggleFavorite(cafeId: Long) {
        val token = authManager.getToken()
        if (token == null) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.FavoriteError("Please log in to follow cafes."))
            }
            return
        }

        val wasFavorite = cafeId in _favoriteCafeIds.value
        _favoriteCafeIds.value = if (wasFavorite) _favoriteCafeIds.value - cafeId else _favoriteCafeIds.value + cafeId

        viewModelScope.launch {
            val result = if (wasFavorite) {
                repository.removeFavorite(token, cafeId)
            } else {
                repository.addFavorite(token, cafeId)
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
        val token = authManager.getToken() ?: return
        viewModelScope.launch {
            val pushToken = pushTokens.current() ?: return@launch
            repository.registerDevice(token, pushToken)
        }
    }
}
