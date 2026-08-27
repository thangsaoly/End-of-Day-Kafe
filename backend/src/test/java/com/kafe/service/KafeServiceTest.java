package com.kafe.service;

import com.kafe.model.Cafe;
import com.kafe.model.Reservation;
import com.kafe.model.SurplusDeal;
import com.kafe.model.User;
import com.kafe.repository.CafeRepository;
import com.kafe.repository.ReservationRepository;
import com.kafe.repository.SurplusDealRepository;
import com.kafe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafeServiceTest {

    @Mock
    private CafeRepository cafeRepository;

    @Mock
    private SurplusDealRepository surplusDealRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private KafeService kafeService;

    private User testUser;
    private SurplusDeal testDeal;
    private Cafe testCafe;

    @BeforeEach
    void setUp() {
        testUser = new User("Sophea", "012345678", "hashed_pass");
        testUser.setId(1L);

        testCafe = new Cafe("Brown Coffee BKK1", 11.55, 104.92, "20:00", "http://example.com/logo.png");
        testCafe.setId(10L);

        testDeal = new SurplusDeal(testCafe, "Croissant Mystery Box", 4.50, 1.50, 3, "18:00", "20:00", "http://example.com/item.png");
        testDeal.setId(100L);
    }

    @Test
    void reserveDeal_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.countByUserAndStatus(testUser, "PENDING")).thenReturn(0L);
        when(surplusDealRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(testDeal));
        when(reservationRepository.findByClaimCode(anyString())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        Reservation reservation = kafeService.reserveDeal(100L, testUser);

        assertNotNull(reservation);
        assertEquals(2, testDeal.getQuantityAvailable());
        assertNotNull(reservation.getClaimCode());
        assertTrue(reservation.getClaimCode().startsWith("KAFE-"));
        verify(surplusDealRepository, times(1)).save(testDeal);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void reserveDeal_ThrowsException_WhenUserIsBanned() {
        testUser.setBanned(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                kafeService.reserveDeal(100L, testUser)
        );

        assertTrue(ex.getMessage().contains("restricted"));
        verify(surplusDealRepository, never()).save(any());
    }

    @Test
    void reserveDeal_ThrowsException_WhenMaxActiveReservationsReached() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.countByUserAndStatus(testUser, "PENDING")).thenReturn(2L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                kafeService.reserveDeal(100L, testUser)
        );

        assertTrue(ex.getMessage().contains("cannot have more than 2 active reservations"));
        verify(surplusDealRepository, never()).save(any());
    }

    @Test
    void reserveDeal_ThrowsException_WhenQuantityZero() {
        testDeal.setQuantityAvailable(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.countByUserAndStatus(testUser, "PENDING")).thenReturn(0L);
        when(surplusDealRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(testDeal));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                kafeService.reserveDeal(100L, testUser)
        );

        assertEquals("No items left for this deal!", ex.getMessage());
        verify(surplusDealRepository, never()).save(any());
    }

    @Test
    void claimReservation_Success() {
        User merchant = new User("Shop Merchant", "099888777", "pass", "MERCHANT");
        merchant.setId(2L);

        Reservation reservation = new Reservation(testDeal, testUser, "KAFE-1234");
        reservation.setId(50L);
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        Reservation result = kafeService.claimReservation(50L, merchant);

        assertEquals("COMPLETED", result.getStatus());
        verify(reservationRepository, times(1)).save(reservation);
    }

    @Test
    void claimReservation_ThrowsSecurityException_WhenNotMerchant() {
        User customer = new User("Other Customer", "088776655", "pass", "CUSTOMER");
        customer.setId(3L);

        SecurityException ex = assertThrows(SecurityException.class, () ->
                kafeService.claimReservation(50L, customer)
        );

        assertTrue(ex.getMessage().contains("Only merchant accounts"));
    }

    @Test
    void claimReservation_ThrowsSecurityException_WhenCustomerTriesSelfClaim() {
        User merchantWhoIsAlsoCustomer = new User("Sophea", "012345678", "pass", "MERCHANT");
        merchantWhoIsAlsoCustomer.setId(1L); // same ID as testUser who placed reservation

        Reservation reservation = new Reservation(testDeal, testUser, "KAFE-1234");
        reservation.setId(50L);
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                kafeService.claimReservation(50L, merchantWhoIsAlsoCustomer)
        );

        assertTrue(ex.getMessage().contains("Customers cannot self-complete"));
    }

    @Test
    void claimReservationByCode_Success() {
        User merchant = new User("Shop Merchant", "099888777", "pass", "MERCHANT");
        merchant.setId(2L);

        Reservation reservation = new Reservation(testDeal, testUser, "KAFE-9999");
        reservation.setId(99L);
        when(reservationRepository.findByClaimCode("KAFE-9999")).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        Reservation result = kafeService.claimReservationByCode("kafe-9999 ", merchant);

        assertEquals("COMPLETED", result.getStatus());
        verify(reservationRepository, times(1)).save(reservation);
    }

    @Test
    void claimReservationByCode_ThrowsException_WhenNotFound() {
        User merchant = new User("Shop Merchant", "099888777", "pass", "MERCHANT");
        merchant.setId(2L);

        when(reservationRepository.findByClaimCode("KAFE-INVALID")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                kafeService.claimReservationByCode("KAFE-INVALID", merchant)
        );

        assertTrue(ex.getMessage().contains("Reservation not found"));
    }

    @Test
    void expireReservation_IncrementsNoShowAndBansUserAtThree() {
        testUser.setNoShowCount(2);
        Reservation reservation = new Reservation(testDeal, testUser, "KAFE-5678");
        reservation.setId(60L);
        when(reservationRepository.findById(60L)).thenReturn(Optional.of(reservation));

        Reservation result = kafeService.expireReservation(60L);

        assertEquals("EXPIRED", result.getStatus());
        assertEquals(3, testUser.getNoShowCount());
        assertTrue(testUser.isBanned());
        verify(userRepository, times(1)).save(testUser);
    }
}
