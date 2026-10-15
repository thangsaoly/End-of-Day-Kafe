package com.kafe.service;

import com.kafe.dto.CreateDealRequest;
import com.kafe.event.DealCreatedEvent;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private KafeService kafeService;

    // 2026-10-15 18:00 in Phnom Penh
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-10-15T11:00:00Z"), ZoneId.of("Asia/Phnom_Penh"));

    private User testUser;
    private User merchant;
    private SurplusDeal testDeal;
    private Cafe testCafe;

    @BeforeEach
    void setUp() {
        kafeService = new KafeService(cafeRepository, surplusDealRepository, reservationRepository, userRepository, fixedClock, eventPublisher);

        testUser = new User("Sophea", "012345678", "hashed_pass");
        testUser.setId(1L);

        testCafe = new Cafe("Brown Coffee BKK1", 11.55, 104.92, "20:00", "http://example.com/logo.png");
        testCafe.setId(10L);

        testDeal = new SurplusDeal(testCafe, "Croissant Mystery Box", 4.50, 1.50, 3, "18:00", "20:00", "http://example.com/item.png");
        testDeal.setId(100L);

        merchant = new User("Shop Merchant", "099888777", "pass", "MERCHANT");
        merchant.setId(2L);
        merchant.setCafeId(10L);
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
        merchantWhoIsAlsoCustomer.setCafeId(10L);

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

        Reservation result = kafeService.expireReservation(60L, merchant);

        assertEquals("EXPIRED", result.getStatus());
        assertEquals(3, testUser.getNoShowCount());
        assertTrue(testUser.isBanned());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void claimReservationByCode_ThrowsSecurityException_WhenReservationBelongsToOtherCafe() {
        merchant.setCafeId(99L);
        Reservation reservation = new Reservation(testDeal, testUser, "KAFE-9999");
        when(reservationRepository.findByClaimCode("KAFE-9999")).thenReturn(Optional.of(reservation));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                kafeService.claimReservationByCode("KAFE-9999", merchant)
        );

        assertTrue(ex.getMessage().contains("different cafe"));
        assertEquals("PENDING", reservation.getStatus());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void claimReservation_ThrowsSecurityException_WhenMerchantHasNoCafe() {
        merchant.setCafeId(null);

        SecurityException ex = assertThrows(SecurityException.class, () ->
                kafeService.claimReservation(50L, merchant)
        );

        assertTrue(ex.getMessage().contains("not linked to a cafe"));
        verify(reservationRepository, never()).findById(any());
    }

    @Test
    void expireReservation_ThrowsSecurityException_WhenCalledByCustomer() {
        User customer = new User("Other Customer", "088776655", "pass", "CUSTOMER");
        customer.setId(3L);

        assertThrows(SecurityException.class, () -> kafeService.expireReservation(60L, customer));

        assertEquals(0, testUser.getNoShowCount());
        verify(userRepository, never()).save(any());
    }

    @Test
    void expireReservation_ThrowsSecurityException_WhenReservationBelongsToOtherCafe() {
        merchant.setCafeId(99L);
        Reservation reservation = new Reservation(testDeal, testUser, "KAFE-5678");
        when(reservationRepository.findById(60L)).thenReturn(Optional.of(reservation));

        assertThrows(SecurityException.class, () -> kafeService.expireReservation(60L, merchant));

        assertEquals("PENDING", reservation.getStatus());
        assertEquals(0, testUser.getNoShowCount());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getMerchantReservations_ReturnsOnlyOwnCafe() {
        when(reservationRepository.findByDealCafeIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        kafeService.getMerchantReservations(merchant);

        verify(reservationRepository).findByDealCafeIdOrderByCreatedAtDesc(10L);
        verify(reservationRepository, never()).findAll();
    }

    @Test
    void getCafeReservations_ThrowsSecurityException_ForOtherCafe() {
        assertThrows(SecurityException.class, () -> kafeService.getCafeReservations(99L, merchant));
        verify(reservationRepository, never()).findByDealCafeIdOrderByCreatedAtDesc(any());
    }

    @Test
    void createDeal_Success_DefaultsPickupDateToTodayInPhnomPenh() {
        when(cafeRepository.findById(10L)).thenReturn(Optional.of(testCafe));
        when(surplusDealRepository.save(any(SurplusDeal.class))).thenAnswer(i -> {
            SurplusDeal saved = i.getArgument(0);
            saved.setId(555L);
            return saved;
        });

        SurplusDeal deal = kafeService.createDeal(dealRequest(6.00, 2.00, null, "19:00", "20:30"), merchant);

        ArgumentCaptor<DealCreatedEvent> event = ArgumentCaptor.forClass(DealCreatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(555L, event.getValue().dealId());

        assertEquals(testCafe, deal.getCafe());
        assertEquals("Mystery Pastry Bag", deal.getTitle());
        assertEquals(3, deal.getQuantityAvailable());
        assertEquals(LocalDate.of(2026, 10, 15), deal.getPickupDate());
    }

    @Test
    void createDeal_ThrowsException_WhenDiscountNotLowerThanOriginal() {
        when(cafeRepository.findById(10L)).thenReturn(Optional.of(testCafe));

        assertThrows(IllegalStateException.class, () ->
                kafeService.createDeal(dealRequest(5.00, 5.00, null, "19:00", "20:30"), merchant));
        verify(surplusDealRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createDeal_ThrowsException_WhenPickupEndNotAfterStart() {
        when(cafeRepository.findById(10L)).thenReturn(Optional.of(testCafe));

        assertThrows(IllegalStateException.class, () ->
                kafeService.createDeal(dealRequest(6.00, 2.00, null, "20:30", "19:00"), merchant));
        verify(surplusDealRepository, never()).save(any());
    }

    @Test
    void createDeal_ThrowsException_WhenPickupDateInPast() {
        when(cafeRepository.findById(10L)).thenReturn(Optional.of(testCafe));

        assertThrows(IllegalStateException.class, () ->
                kafeService.createDeal(dealRequest(6.00, 2.00, LocalDate.of(2026, 10, 14), "19:00", "20:30"), merchant));
        verify(surplusDealRepository, never()).save(any());
    }

    @Test
    void createDeal_ThrowsSecurityException_WhenNotMerchant() {
        assertThrows(SecurityException.class, () ->
                kafeService.createDeal(dealRequest(6.00, 2.00, null, "19:00", "20:30"), testUser));
        verify(surplusDealRepository, never()).save(any());
    }

    private CreateDealRequest dealRequest(double original, double discount, LocalDate date, String start, String end) {
        CreateDealRequest request = new CreateDealRequest();
        request.setTitle("Mystery Pastry Bag");
        request.setOriginalPrice(original);
        request.setDiscountPrice(discount);
        request.setQuantity(3);
        request.setPickupDate(date);
        request.setPickupStart(start);
        request.setPickupEnd(end);
        return request;
    }
}
