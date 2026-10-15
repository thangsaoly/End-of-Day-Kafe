package com.kafe.android.viewmodel

import com.kafe.android.auth.AuthClient
import com.kafe.android.model.Cafe
import com.kafe.android.model.Reservation
import com.kafe.android.model.SurplusDeal
import com.kafe.android.network.AuthManager
import com.kafe.android.network.AuthResponse
import com.kafe.android.push.PushTokens
import com.kafe.android.repository.KafeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class KafeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: KafeRepository
    private lateinit var authManager: AuthManager
    private lateinit var authClient: FakeAuthClient

    private val sampleCafe = Cafe(1L, "Brown Coffee BKK1", 11.55, 104.92, "20:00", null)
    private val sampleDeal = SurplusDeal(10L, sampleCafe, "Croissant Bag", 5.0, 2.0, 3, "18:00", "20:00", null)
    private val customerProfile = AuthResponse(5L, "Sophea", null, "sophea@example.com", true, "CUSTOMER", null)

    private val fakePushTokens = object : PushTokens {
        override suspend fun current(): String = "push-123"
    }

    /** Records calls; fails sign-in with [signInError] when set. */
    private class FakeAuthClient : AuthClient {
        override var isSignedIn = false
        var signInError: Exception? = null
        val calls = mutableListOf<String>()

        override suspend fun signInWithEmail(email: String, password: String) = signIn("email:$email")
        override suspend fun signUpWithEmail(email: String, password: String, displayName: String) = signIn("signup:$email:$displayName")
        override suspend fun signInWithGoogle(googleIdToken: String) = signIn("google:$googleIdToken")
        override suspend fun sendPasswordReset(email: String) { calls += "reset:$email" }
        override suspend fun idToken(): String? = if (isSignedIn) "firebase-id-token" else null
        override fun signOut() {
            calls += "signOut"
            isSignedIn = false
        }

        private fun signIn(call: String) {
            calls += call
            signInError?.let { throw it }
            isSignedIn = true
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        authManager = mock()
        authClient = FakeAuthClient()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun loggedOutViewModel(): KafeViewModel {
        whenever(repository.getDeals()).doReturn(Result.success(listOf(sampleDeal)))
        whenever(authManager.isLoggedIn()).doReturn(false)
        return KafeViewModel(repository, authManager, authClient)
    }

    private suspend fun loggedInViewModel(pushTokens: PushTokens = PushTokens.None): KafeViewModel {
        authClient.isSignedIn = true
        whenever(repository.getDeals()).doReturn(Result.success(listOf(sampleDeal)))
        whenever(authManager.isLoggedIn()).doReturn(true)
        whenever(authManager.getUserEmail()).doReturn("sophea@example.com")
        whenever(authManager.getUserRole()).doReturn("CUSTOMER")
        whenever(repository.getReservations()).doReturn(Result.success(emptyList()))
        whenever(repository.getMerchantReservations()).doReturn(Result.success(emptyList()))
        whenever(repository.getFavorites()).doReturn(Result.success(emptyList()))
        whenever(repository.registerDevice("push-123")).doReturn(Result.success(Unit))
        return KafeViewModel(repository, authManager, authClient, pushTokens)
    }

    // Deals

    @Test
    fun loadDeals_Success_UpdatesUiStateToSuccess() = runTest {
        val viewModel = loggedOutViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("Croissant Bag", (state as UiState.Success).deals.single().title)
    }

    @Test
    fun loadDeals_Failure_UpdatesUiStateToError() = runTest {
        whenever(repository.getDeals()).doReturn(Result.failure(Exception("Network unreachable")))
        whenever(authManager.isLoggedIn()).doReturn(false)

        val viewModel = KafeViewModel(repository, authManager, authClient)
        advanceUntilIdle()

        assertEquals("Network unreachable", (viewModel.uiState.value as UiState.Error).message)
    }

    @Test
    fun reserveDeal_WhenNotLoggedIn_EmitsReservationError() = runTest {
        val viewModel = loggedOutViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.reserveDeal(10L)
        advanceUntilIdle()

        assertEquals(UiEvent.ReservationError("Please log in to make a reservation."), events.single())
        verify(repository, never()).reserveDeal(any())
    }

    // Startup

    @Test
    fun init_StaleProfileWithoutFirebaseSession_IsCleared() = runTest {
        whenever(repository.getDeals()).doReturn(Result.success(emptyList()))
        whenever(authManager.isLoggedIn()).doReturn(true, false)
        authClient.isSignedIn = false

        val viewModel = KafeViewModel(repository, authManager, authClient)
        advanceUntilIdle()

        verify(authManager).clearAuth()
        assertFalse(viewModel.isLoggedIn)
        verify(repository, never()).getReservations()
    }

    @Test
    fun init_LoggedIn_LoadsDataAndRegistersPushToken() = runTest {
        val viewModel = loggedInViewModel(fakePushTokens)
        advanceUntilIdle()

        assertTrue(viewModel.isLoggedIn)
        assertEquals("sophea@example.com", viewModel.userEmail)
        verify(repository).getReservations()
        verify(repository).getFavorites()
        verify(repository).registerDevice("push-123")
    }

    @Test
    fun init_NoPushToken_DoesNotRegister() = runTest {
        loggedInViewModel(PushTokens.None)
        advanceUntilIdle()

        verify(repository, never()).registerDevice(any())
    }

    // Sign-in

    @Test
    fun login_FirebaseThenBackendSession_SavesProfile() = runTest {
        val viewModel = loggedOutViewModel()
        whenever(repository.startSession(null, null)).doReturn(Result.success(customerProfile))
        whenever(repository.getReservations()).doReturn(Result.success(emptyList()))
        whenever(repository.getFavorites()).doReturn(Result.success(emptyList()))
        whenever(authManager.getUserEmail()).doReturn("sophea@example.com")
        whenever(authManager.getUserRole()).doReturn("CUSTOMER")
        advanceUntilIdle()

        var result: Result<Unit>? = null
        viewModel.login("sophea@example.com", "secret1") { result = it }
        advanceUntilIdle()

        assertTrue(result!!.isSuccess)
        assertEquals(listOf("email:sophea@example.com"), authClient.calls)
        verify(authManager).saveProfile(customerProfile)
        assertTrue(viewModel.isLoggedIn)
    }

    @Test
    fun login_FirebaseRejects_DoesNotCallBackend() = runTest {
        val viewModel = loggedOutViewModel()
        authClient.signInError = Exception("Incorrect email or password.")
        advanceUntilIdle()

        var result: Result<Unit>? = null
        viewModel.login("sophea@example.com", "wrong") { result = it }
        advanceUntilIdle()

        assertEquals("Incorrect email or password.", result!!.exceptionOrNull()!!.message)
        verify(repository, never()).startSession(any(), any())
        assertFalse(viewModel.isLoggedIn)
    }

    @Test
    fun login_BackendRefuses_SignsOutOfFirebase() = runTest {
        val viewModel = loggedOutViewModel()
        whenever(repository.startSession(null, null))
            .doReturn(Result.failure(Exception("Your account has been banned due to multiple no-shows!")))
        advanceUntilIdle()

        var result: Result<Unit>? = null
        viewModel.login("sophea@example.com", "secret1") { result = it }
        advanceUntilIdle()

        assertTrue(result!!.exceptionOrNull()!!.message!!.contains("banned"))
        assertEquals("signOut", authClient.calls.last())
        assertFalse(authClient.isSignedIn)
        verify(authManager, never()).saveProfile(any())
        assertFalse(viewModel.isLoggedIn)
    }

    @Test
    fun register_SendsNameAndRoleToBackend() = runTest {
        val viewModel = loggedOutViewModel()
        val merchantProfile = customerProfile.copy(role = "MERCHANT")
        whenever(repository.startSession("Dara", "MERCHANT")).doReturn(Result.success(merchantProfile))
        whenever(repository.getMerchantReservations()).doReturn(Result.success(emptyList()))
        whenever(repository.getFavorites()).doReturn(Result.success(emptyList()))
        whenever(authManager.getUserRole()).doReturn("MERCHANT")
        whenever(authManager.isMerchantRole()).doReturn(true)
        advanceUntilIdle()

        viewModel.register("Dara", "dara@example.com", "secret1", "MERCHANT") { }
        advanceUntilIdle()

        assertEquals(listOf("signup:dara@example.com:Dara"), authClient.calls)
        verify(authManager).saveProfile(merchantProfile)
        assertTrue(viewModel.isMerchantMode)
    }

    @Test
    fun signInWithGoogle_PassesRoleForNewAccounts() = runTest {
        val viewModel = loggedOutViewModel()
        whenever(repository.startSession(null, "CUSTOMER")).doReturn(Result.success(customerProfile))
        whenever(repository.getReservations()).doReturn(Result.success(emptyList()))
        whenever(repository.getFavorites()).doReturn(Result.success(emptyList()))
        whenever(authManager.getUserRole()).doReturn("CUSTOMER")
        advanceUntilIdle()

        viewModel.signInWithGoogle("google-id-token", "CUSTOMER") { }
        advanceUntilIdle()

        assertEquals(listOf("google:google-id-token"), authClient.calls)
        assertTrue(viewModel.isLoggedIn)
    }

    @Test
    fun forgotPassword_SendsFirebaseResetEmail() = runTest {
        val viewModel = loggedOutViewModel()
        advanceUntilIdle()

        var result: Result<String>? = null
        viewModel.forgotPassword("sophea@example.com") { result = it }
        advanceUntilIdle()

        assertEquals(listOf("reset:sophea@example.com"), authClient.calls)
        assertTrue(result!!.getOrThrow().contains("sophea@example.com"))
    }

    @Test
    fun logout_UnregistersPushTokenBeforeFirebaseSignOut() = runTest {
        val viewModel = loggedInViewModel(fakePushTokens)
        whenever(repository.unregisterDevice("push-123")).doReturn(Result.success(Unit))
        advanceUntilIdle()

        viewModel.logout()
        assertFalse(viewModel.isLoggedIn) // immediately, before the network call
        advanceUntilIdle()

        verify(repository).unregisterDevice("push-123")
        verify(authManager).clearAuth()
        assertEquals("signOut", authClient.calls.last())
        assertTrue(viewModel.favoriteCafeIds.value.isEmpty())
    }

    // Merchant

    @Test
    fun toggleMerchantMode_SwitchesState() = runTest {
        val viewModel = loggedInViewModel()
        advanceUntilIdle()

        viewModel.toggleMerchantMode()

        assertTrue(viewModel.isMerchantMode)
    }

    @Test
    fun claimReservationByCode_Success_EmitsClaimSuccess() = runTest {
        val viewModel = loggedInViewModel()
        val sampleUser = com.kafe.android.model.User(1L, "Sophea", null, "sophea@example.com", true, 0, false)
        val claimed = Reservation(100L, sampleDeal, sampleUser, "KAFE-1234", "COMPLETED", "2026-08-06")
        whenever(repository.claimReservationByCode("KAFE-1234")).doReturn(Result.success(claimed))
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.claimReservationByCode("KAFE-1234")
        advanceUntilIdle()

        assertEquals("COMPLETED", (events.single() as UiEvent.ClaimSuccess).reservation.status)
    }

    // Favourites

    @Test
    fun toggleFavorite_Success_AddsCafeImmediately() = runTest {
        val viewModel = loggedInViewModel()
        whenever(repository.addFavorite(1L)).doReturn(Result.success(Unit))
        advanceUntilIdle()

        viewModel.toggleFavorite(1L)
        assertTrue(1L in viewModel.favoriteCafeIds.value) // optimistic, before the call finishes
        advanceUntilIdle()

        assertTrue(1L in viewModel.favoriteCafeIds.value)
        verify(repository).addFavorite(1L)
    }

    @Test
    fun toggleFavorite_Failure_RevertsAndEmitsError() = runTest {
        val viewModel = loggedInViewModel()
        whenever(repository.addFavorite(1L)).doReturn(Result.failure(Exception("Server down")))
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.toggleFavorite(1L)
        advanceUntilIdle()

        assertFalse(1L in viewModel.favoriteCafeIds.value)
        assertEquals(UiEvent.FavoriteError("Server down"), events.single())
    }

    @Test
    fun toggleFavorite_WhenAlreadyFavorite_Removes() = runTest {
        val viewModel = loggedInViewModel()
        whenever(repository.getFavorites()).doReturn(Result.success(listOf(sampleCafe)))
        whenever(repository.removeFavorite(1L)).doReturn(Result.success(Unit))
        viewModel.loadFavorites()
        advanceUntilIdle()

        viewModel.toggleFavorite(1L)
        advanceUntilIdle()

        assertFalse(1L in viewModel.favoriteCafeIds.value)
        verify(repository).removeFavorite(1L)
    }

    @Test
    fun openDeal_SetsFocusAndReloadsDeals_ThenClears() = runTest {
        val viewModel = loggedOutViewModel()
        advanceUntilIdle()

        viewModel.openDeal(10L)
        assertEquals(10L, viewModel.focusedDealId.value)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)

        viewModel.onDealFocused()
        assertNull(viewModel.focusedDealId.value)
    }

    private fun kotlinx.coroutines.test.TestScope.collectEvents(viewModel: KafeViewModel): MutableList<UiEvent> {
        val events = mutableListOf<UiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }
        return events
    }
}
