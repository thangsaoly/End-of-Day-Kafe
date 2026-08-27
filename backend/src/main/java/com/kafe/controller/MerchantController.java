package com.kafe.controller;

import com.kafe.model.Reservation;
import com.kafe.model.User;
import com.kafe.service.KafeService;
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
        return ResponseEntity.ok(kafeService.getCafeReservations(cafeId));
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<Reservation>> getAllReservations() {
        return ResponseEntity.ok(kafeService.getAllReservations());
    }

    @PostMapping("/reservations/{reservationId}/claim")
    public ResponseEntity<?> claimReservation(@PathVariable Long reservationId) {
        try {
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Reservation updated = kafeService.claimReservation(reservationId, currentUser);
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
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Reservation updated = kafeService.claimReservationByCode(claimCode, currentUser);
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
            Reservation updated = kafeService.expireReservation(reservationId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
