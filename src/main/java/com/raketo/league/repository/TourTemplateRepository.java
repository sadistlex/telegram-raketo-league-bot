package com.raketo.league.repository;

import com.raketo.league.model.TourTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourTemplateRepository extends JpaRepository<TourTemplate, Long> {
    List<TourTemplate> findByDivisionTournamentId(Long divisionTournamentId);
}
