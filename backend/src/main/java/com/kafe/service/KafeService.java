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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class KafeService {

    private final CafeRepository cafeRepository;
    private final SurplusDealRepository surplusDealRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public KafeService(CafeRepository cafeRepository, 
                       SurplusDealRepository surplusDealRepository,
                       ReservationRepository reservationRepository,
                       UserRepository userRepository,
                       Clock clock,
                       ApplicationEventPublisher eventPublisher) {
        this.cafeRepository = cafeRepository;
        this.surplusDealRepository = surplusDealRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    public List<Cafe> getAllCafes() {
        return cafeRepository.findAll();
    }

    public List<SurplusDeal> getAllDeals() {
        return surplusDealRepository.findAll();
    }

    public List<Reservation> getUserReservations(User user) {
        return reservationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Reservation> getCafeReservations(Long cafeId, User currentUser) {
        Long merchantCafeId = requireMerchantCafe(currentUser);
        if (!merchantCafeId.equals(cafeId)) {
            throw new SecurityException("Access denied: You can only view reservations for your own cafe.");
        }
        return reservationRepository.findByDealCafeIdOrderByCreatedAtDesc(cafeId);
    }

    public List<Reservation> getMerchantReservations(User currentUser) {
        Long merchantCafeId = requireMerchantCafe(currentUser);
        return reservationRepository.findByDealCafeIdOrderByCreatedAtDesc(merchantCafeId);
    }

    @Transactional
    public SurplusDeal createDeal(CreateDealRequest request, User currentUser) {
        Long merchantCafeId = requireMerchantCafe(currentUser);
        Cafe cafe = cafeRepository.findById(merchantCafeId)
                .orElseThrow(() -> new IllegalArgumentException("Cafe not found: " + merchantCafeId));

        if (request.getDiscountPrice() >= request.getOriginalPrice()) {
            throw new IllegalStateException("Discount price must be lower than the original price.");
        }
        // HH:mm strings compare correctly as text
        if (request.getPickupEnd().compareTo(request.getPickupStart()) <= 0) {
            throw new IllegalStateException("Pickup end time must be after the pickup start time.");
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate pickupDate = request.getPickupDate() != null ? request.getPickupDate() : today;
        if (pickupDate.isBefore(today)) {
            throw new IllegalStateException("Pickup date cannot be in the past.");
        }

        SurplusDeal deal = new SurplusDeal(
                cafe,
                request.getTitle().trim(),
                request.getOriginalPrice(),
                request.getDiscountPrice(),
                request.getQuantity(),
                request.getPickupStart(),
                request.getPickupEnd(),
                request.getImageUrl()
        );
        deal.setPickupDate(pickupDate);
        SurplusDeal saved = surplusDealRepository.save(deal);
        eventPublisher.publishEvent(new DealCreatedEvent(saved.getId()));
        return saved;
    }

    @Transactional
    public Reservation reserveDeal(Long dealId, User user) {
        // Refetch user to get the latest DB state (isBanned/noShowCount)
        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 1. Abuse prevention: Check if user is banned
        if (dbUser.isBanned()) {
            throw new IllegalStateException("Your account is restricted from making reservations due to multiple no-shows.");
        }

        // 2. Abuse prevention: Check if user already has 2 active (PENDING) reservations
        long activeCount = reservationRepository.countByUserAndStatus(dbUser, "PENDING");
        if (activeCount >= 2) {
            throw new IllegalStateException("You cannot have more than 2 active reservations at the same time.");
        }

        // 3. Fetch deal with PESSIMISTIC_WRITE lock (concurrency control)
        SurplusDeal deal = surplusDealRepository.findByIdForUpdate(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealId));

        if (deal.getQuantityAvailable() <= 0) {
            throw new IllegalStateException("No items left for this deal!");
        }

        // 4. Decrement quantity
        deal.setQuantityAvailable(deal.getQuantityAvailable() - 1);
        surplusDealRepository.save(deal);

        // 5. Generate unique claim code
        String claimCode = "KAFE-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        while (reservationRepository.findByClaimCode(claimCode).isPresent()) {
            claimCode = "KAFE-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }

        // 6. Create and save reservation
        Reservation reservation = new Reservation(deal, dbUser, claimCode);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation claimReservation(Long reservationId, User currentUser) {
        requireMerchantCafe(currentUser);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Access denied: Customers cannot self-complete or claim their own reservations.");
        }
        requireOwnCafe(currentUser, reservation);

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new IllegalStateException("Reservation is already processed: " + reservation.getStatus());
        }

        reservation.setStatus("COMPLETED");
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation claimReservationByCode(String claimCode, User currentUser) {
        requireMerchantCafe(currentUser);

        if (claimCode == null || claimCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Claim code cannot be empty");
        }
        String formattedCode = claimCode.trim().toUpperCase();
        Reservation reservation = reservationRepository.findByClaimCode(formattedCode)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found for claim code: " + claimCode));

        if (reservation.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Access denied: Customers cannot self-complete or claim their own reservations.");
        }
        requireOwnCafe(currentUser, reservation);

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new IllegalStateException("Reservation is already processed: " + reservation.getStatus());
        }

        reservation.setStatus("COMPLETED");
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation expireReservation(Long reservationId, User currentUser) {
        requireMerchantCafe(currentUser);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        requireOwnCafe(currentUser, reservation);

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new IllegalStateException("Reservation is already processed: " + reservation.getStatus());
        }

        reservation.setStatus("EXPIRED");
        reservationRepository.save(reservation);

        // Increment no-show counter
        User user = reservation.getUser();
        user.setNoShowCount(user.getNoShowCount() + 1);
        if (user.getNoShowCount() >= 3) {
            user.setBanned(true);
        }
        userRepository.save(user);

        return reservation;
    }

    // Returns the merchant's cafe id, or throws if the user is not a merchant linked to a cafe
    private Long requireMerchantCafe(User user) {
        if (user == null || !"MERCHANT".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Access denied: Only merchant accounts can manage cafe reservations and deals.");
        }
        if (user.getCafeId() == null) {
            throw new SecurityException("Access denied: This merchant account is not linked to a cafe yet.");
        }
        return user.getCafeId();
    }

    private void requireOwnCafe(User merchant, Reservation reservation) {
        if (!merchant.getCafeId().equals(reservation.getDeal().getCafe().getId())) {
            throw new SecurityException("Access denied: This reservation belongs to a different cafe.");
        }
    }
}
