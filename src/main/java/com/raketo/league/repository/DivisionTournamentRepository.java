package com.raketo.league.repository;

import com.raketo.league.model.DivisionTournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DivisionTournamentRepository extends JpaRepository<DivisionTournament, Long> {
    List<DivisionTournament> findByTournamentId(Long tournamentId);
    List<DivisionTournament> findByDivisionId(Long divisionId);
}
