package com.kafe.repository;

import com.kafe.model.Reservation;
import com.kafe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByClaimCode(String claimCode);

    long countByUserAndStatus(User user, String status);

    List<Reservation> findByUserOrderByCreatedAtDesc(User user);

    List<Reservation> findByDealCafeIdOrderByCreatedAtDesc(Long cafeId);
}
