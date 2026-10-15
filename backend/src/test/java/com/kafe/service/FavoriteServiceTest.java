package com.kafe.service;

import com.kafe.model.Cafe;
import com.kafe.model.Favorite;
import com.kafe.model.User;
import com.kafe.repository.CafeRepository;
import com.kafe.repository.FavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private CafeRepository cafeRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private Cafe cafe;

    @BeforeEach
    void setUp() {
        user = new User("Sophea", "012345678", "hash");
        user.setId(1L);
        cafe = new Cafe("Brown Coffee BKK1", 11.55, 104.92, "21:00", null);
        cafe.setId(10L);
    }

    @Test
    void addFavorite_SavesNewFavorite() {
        when(cafeRepository.findById(10L)).thenReturn(Optional.of(cafe));
        when(favoriteRepository.findByUserIdAndCafeId(1L, 10L)).thenReturn(Optional.empty());

        Cafe result = favoriteService.addFavorite(user, 10L);

        assertEquals(cafe, result);
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void addFavorite_IsIdempotent() {
        when(cafeRepository.findById(10L)).thenReturn(Optional.of(cafe));
        when(favoriteRepository.findByUserIdAndCafeId(1L, 10L)).thenReturn(Optional.of(new Favorite(user, cafe)));

        favoriteService.addFavorite(user, 10L);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void addFavorite_ThrowsWhenCafeMissing() {
        when(cafeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> favoriteService.addFavorite(user, 99L));
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void removeFavorite_DeletesOnlyThatUsersFavorite() {
        Favorite favorite = new Favorite(user, cafe);
        when(favoriteRepository.findByUserIdAndCafeId(1L, 10L)).thenReturn(Optional.of(favorite));

        favoriteService.removeFavorite(user, 10L);

        verify(favoriteRepository).delete(favorite);
    }

    @Test
    void getFavoriteCafes_ReturnsCafes() {
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(new Favorite(user, cafe)));

        assertEquals(List.of(cafe), favoriteService.getFavoriteCafes(user));
    }
}
