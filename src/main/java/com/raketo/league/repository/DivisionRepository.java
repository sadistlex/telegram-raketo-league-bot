package com.raketo.league.repository;

import com.raketo.league.model.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DivisionRepository extends JpaRepository<Division, Long> {

    List<Division> findByTournamentId(Long tournamentId);

    List<Division> findByTournamentIdAndIsActive(Long tournamentId, Boolean isActive);
}

