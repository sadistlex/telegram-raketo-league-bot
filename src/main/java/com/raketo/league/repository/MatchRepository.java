package com.raketo.league.repository;

import com.raketo.league.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTourId(Long tourId);

    @Query("SELECT m FROM Match m WHERE (m.player1.id = :playerId OR m.player2.id = :playerId)")
    List<Match> findByPlayerId(@Param("playerId") Long playerId);

    @Query("SELECT m FROM Match m WHERE (m.player1.id = :playerId OR m.player2.id = :playerId) AND m.status = :status")
    List<Match> findByPlayerIdAndStatus(@Param("playerId") Long playerId, @Param("status") Match.MatchStatus status);

    List<Match> findByStatus(Match.MatchStatus status);
}

