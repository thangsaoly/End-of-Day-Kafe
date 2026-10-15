package com.kafe.controller;

import com.kafe.model.Cafe;
import com.kafe.model.User;
import com.kafe.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ResponseEntity<List<Cafe>> getFavorites() {
        return ResponseEntity.ok(favoriteService.getFavoriteCafes(currentUser()));
    }

    @PostMapping("/{cafeId}")
    public ResponseEntity<Cafe> addFavorite(@PathVariable Long cafeId) {
        return ResponseEntity.ok(favoriteService.addFavorite(currentUser(), cafeId));
    }

    @DeleteMapping("/{cafeId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long cafeId) {
        favoriteService.removeFavorite(currentUser(), cafeId);
        return ResponseEntity.noContent().build();
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
