package com.raketo.league.repository;

import com.raketo.league.model.TourPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourPlayerRepository extends JpaRepository<TourPlayer, Long> {
    List<TourPlayer> findByTourId(Long tourId);
    List<TourPlayer> findByPlayerId(Long playerId);
}
