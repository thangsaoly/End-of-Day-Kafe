package com.kafe.service;

import com.kafe.model.Cafe;
import com.kafe.model.Favorite;
import com.kafe.model.User;
import com.kafe.repository.CafeRepository;
import com.kafe.repository.FavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final CafeRepository cafeRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, CafeRepository cafeRepository) {
        this.favoriteRepository = favoriteRepository;
        this.cafeRepository = cafeRepository;
    }

    public List<Cafe> getFavoriteCafes(User user) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(Favorite::getCafe)
                .toList();
    }

    // Idempotent: favouriting an already-favourite cafe is a no-op
    @Transactional
    public Cafe addFavorite(User user, Long cafeId) {
        Cafe cafe = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("Cafe not found: " + cafeId));
        if (favoriteRepository.findByUserIdAndCafeId(user.getId(), cafeId).isEmpty()) {
            favoriteRepository.save(new Favorite(user, cafe));
        }
        return cafe;
    }

    @Transactional
    public void removeFavorite(User user, Long cafeId) {
        favoriteRepository.findByUserIdAndCafeId(user.getId(), cafeId)
                .ifPresent(favoriteRepository::delete);
    }
}
