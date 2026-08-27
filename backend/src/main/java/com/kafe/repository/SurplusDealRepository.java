package com.kafe.repository;

import com.kafe.model.SurplusDeal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SurplusDealRepository extends JpaRepository<SurplusDeal, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SurplusDeal s WHERE s.id = :id")
    Optional<SurplusDeal> findByIdForUpdate(@Param("id") Long id);
}
