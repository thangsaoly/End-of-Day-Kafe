package com.kafe.controller;

import com.kafe.dto.CreateDealRequest;
import com.kafe.model.Reservation;
import com.kafe.model.SurplusDeal;
import com.kafe.model.User;
import com.kafe.service.KafeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final KafeService kafeService;

    public MerchantController(KafeService kafeService) {
        this.kafeService = kafeService;
    }

    @GetMapping("/cafes/{cafeId}/reservations")
    public ResponseEntity<List<Reservation>> getCafeReservations(@PathVariable Long cafeId) {
        return ResponseEntity.ok(kafeService.getCafeReservations(cafeId, currentUser()));
    }

    // Reservations for the logged-in merchant's own cafe only
    @GetMapping("/reservations")
    public ResponseEntity<List<Reservation>> getMyCafeReservations() {
        return ResponseEntity.ok(kafeService.getMerchantReservations(currentUser()));
    }

    @PostMapping("/deals")
    public ResponseEntity<SurplusDeal> createDeal(@Valid @RequestBody CreateDealRequest request) {
        SurplusDeal created = kafeService.createDeal(request, currentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/reservations/{reservationId}/claim")
    public ResponseEntity<?> claimReservation(@PathVariable Long reservationId) {
        try {
            Reservation updated = kafeService.claimReservation(reservationId, currentUser());
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/reservations/claim-by-code/{claimCode}")
    public ResponseEntity<?> claimReservationByCode(@PathVariable String claimCode) {
        try {
            Reservation updated = kafeService.claimReservationByCode(claimCode, currentUser());
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/reservations/{reservationId}/expire")
    public ResponseEntity<?> expireReservation(@PathVariable Long reservationId) {
        try {
            Reservation updated = kafeService.expireReservation(reservationId, currentUser());
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
