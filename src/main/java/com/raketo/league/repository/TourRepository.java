package com.raketo.league.repository;

import com.raketo.league.model.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {

    List<Tour> findByDivisionId(Long divisionId);

    List<Tour> findByDivisionIdOrderByTourNumberAsc(Long divisionId);

    List<Tour> findByStatus(Tour.TourStatus status);
}

