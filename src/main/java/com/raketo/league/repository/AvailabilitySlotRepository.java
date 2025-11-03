package com.raketo.league.repository;

import com.raketo.league.model.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    List<AvailabilitySlot> findByPlayerId(Long playerId);

    List<AvailabilitySlot> findByMatchId(Long matchId);

    @Query("SELECT a FROM AvailabilitySlot a WHERE a.player.id = :playerId " +
           "AND a.startTime >= :startDate AND a.endTime <= :endDate")
    List<AvailabilitySlot> findByPlayerIdAndDateRange(
            @Param("playerId") Long playerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

