package com.kafe.repository;

import com.kafe.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Favorite> findByUserIdAndCafeId(Long userId, Long cafeId);

    @Query("SELECT f.user.id FROM Favorite f WHERE f.cafe.id = :cafeId")
    List<Long> findUserIdsByCafeId(@Param("cafeId") Long cafeId);
}
