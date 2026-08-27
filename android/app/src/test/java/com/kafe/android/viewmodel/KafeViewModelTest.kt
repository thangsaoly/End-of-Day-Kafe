package com.kafe.android.viewmodel

import com.kafe.android.model.Cafe
import com.kafe.android.model.Reservation
import com.kafe.android.model.SurplusDeal
import com.kafe.android.network.AuthManager
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class KafeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: KafeRepository
    private lateinit var authManager: AuthManager

    private val sampleCafe = Cafe(1L, "Brown Coffee BKK1", 11.55, 104.92, "20:00", null)
    private val sampleDeal = SurplusDeal(10L, sampleCafe, "Croissant Bag", 5.0, 2.0, 3, "18:00", "20:00", null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        authManager = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDeals_Success_UpdatesUiStateToSuccess() = runTest {
        whenever(repository.getDeals()).doReturn(Result.success(listOf(sampleDeal)))
        whenever(authManager.isLoggedIn()).doReturn(false)

        val viewModel = KafeViewModel(repository, authManager)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(1, (state as UiState.Success).deals.size)
        assertEquals("Croissant Bag", state.deals[0].title)
    }

    @Test
    fun loadDeals_Failure_UpdatesUiStateToError() = runTest {
        whenever(repository.getDeals()).doReturn(Result.failure(Exception("Network unreachable")))
        whenever(authManager.isLoggedIn()).doReturn(false)

        val viewModel = KafeViewModel(repository, authManager)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Network unreachable", (state as UiState.Error).message)
    }

    @Test
    fun reserveDeal_WhenNotLoggedIn_EmitsReservationError() = runTest {
        whenever(repository.getDeals()).doReturn(Result.success(listOf(sampleDeal)))
        whenever(authManager.isLoggedIn()).doReturn(false)
        whenever(authManager.getToken()).doReturn(null)

        val viewModel = KafeViewModel(repository, authManager)
        advanceUntilIdle()

        val events = mutableListOf<UiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        viewModel.reserveDeal(10L)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is UiEvent.ReservationError)
        assertEquals("Please log in to make a reservation.", (events.first() as UiEvent.ReservationError).message)
    }

    @Test
    fun toggleMerchantMode_SwitchesState() = runTest {
        whenever(repository.getDeals()).doReturn(Result.success(listOf(sampleDeal)))
        whenever(authManager.isLoggedIn()).doReturn(true)
        whenever(authManager.getToken()).doReturn("mock_token")
        whenever(repository.getReservations("mock_token")).doReturn(Result.success(emptyList()))
        whenever(repository.getMerchantReservations("mock_token")).doReturn(Result.success(emptyList()))

        val viewModel = KafeViewModel(repository, authManager)
        advanceUntilIdle()

        val initialMode = viewModel.isMerchantMode
        viewModel.toggleMerchantMode()

        assertEquals(false, initialMode)
        assertEquals(true, viewModel.isMerchantMode)
    }

    @Test
    fun claimReservationByCode_Success_EmitsClaimSuccess() = runTest {
        whenever(repository.getDeals()).doReturn(Result.success(listOf(sampleDeal)))
        whenever(authManager.isLoggedIn()).doReturn(true)
        whenever(authManager.getToken()).doReturn("mock_token")
        whenever(repository.getReservations("mock_token")).doReturn(Result.success(emptyList()))
        whenever(repository.getMerchantReservations("mock_token")).doReturn(Result.success(emptyList()))

        val sampleUser = com.kafe.android.model.User(1L, "Sophea", "012345678", "sophea@example.com", true, 0, false)
        val claimedReservation = Reservation(100L, sampleDeal, sampleUser, "KAFE-1234", "COMPLETED", "2026-08-06")

        whenever(repository.claimReservationByCode("mock_token", "KAFE-1234"))
            .doReturn(Result.success(claimedReservation))

        val viewModel = KafeViewModel(repository, authManager)
        advanceUntilIdle()

        val events = mutableListOf<UiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        viewModel.claimReservationByCode("KAFE-1234")
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is UiEvent.ClaimSuccess)
        assertEquals("COMPLETED", (events.first() as UiEvent.ClaimSuccess).reservation.status)
    }
}
