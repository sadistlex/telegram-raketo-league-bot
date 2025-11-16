package com.raketo.league.repository;

import com.raketo.league.model.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {
    @Query("SELECT t FROM Tour t WHERE t.tourTemplate.divisionTournament.division.id = :divisionId")
    List<Tour> findByDivisionId(@Param("divisionId") Long divisionId);

    List<Tour> findByTourTemplateId(Long tourTemplateId);
}
