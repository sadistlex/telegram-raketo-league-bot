package com.raketo.league.repository;

import com.raketo.league.model.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    List<AvailabilitySlot> findByPlayerId(Long playerId);
    List<AvailabilitySlot> findByTourId(Long tourId);
    @Query("SELECT a FROM AvailabilitySlot a WHERE a.player.id = :playerId AND a.tour.id = :tourId")
    List<AvailabilitySlot> findByPlayerIdAndTourId(@Param("playerId") Long playerId, @Param("tourId") Long tourId);
}
