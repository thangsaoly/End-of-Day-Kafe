package com.kafe.controller;

import com.kafe.model.Reservation;
import com.kafe.model.SurplusDeal;
import com.kafe.model.User;
import com.kafe.service.KafeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class KafeController {

    private final KafeService kafeService;

    public KafeController(KafeService kafeService) {
        this.kafeService = kafeService;
    }

    @GetMapping("/deals")
    public ResponseEntity<List<SurplusDeal>> getAllDeals() {
        return ResponseEntity.ok(kafeService.getAllDeals());
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<Reservation>> getMyReservations() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(kafeService.getUserReservations(user));
    }

    @PostMapping("/deals/{id}/reserve")
    public ResponseEntity<Reservation> reserveDeal(@PathVariable Long id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Reservation reservation = kafeService.reserveDeal(id, user);
        return ResponseEntity.ok(reservation);
    }
}
