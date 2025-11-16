package com.raketo.league.repository;

import com.raketo.league.model.PlayerDivisionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerDivisionAssignmentRepository extends JpaRepository<PlayerDivisionAssignment, Long> {
    List<PlayerDivisionAssignment> findByDivisionTournamentId(Long divisionTournamentId);
    List<PlayerDivisionAssignment> findByPlayerId(Long playerId);
    List<PlayerDivisionAssignment> findByPlayerIdAndDivisionTournamentId(Long playerId, Long divisionTournamentId);
}
