package com.raketo.league.service;

import com.raketo.league.model.Match;
import com.raketo.league.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);

    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public List<Match> getPlayerMatches(Long playerId) {
        return matchRepository.findByPlayerId(playerId);
    }

    @Transactional(readOnly = true)
    public List<Match> getPlayerMatchesByStatus(Long playerId, Match.MatchStatus status) {
        return matchRepository.findByPlayerIdAndStatus(playerId, status);
    }

    @Transactional(readOnly = true)
    public List<Match> getTourMatches(Long tourId) {
        return matchRepository.findByTourId(tourId);
    }

    @Transactional
    public Match updateMatchStatus(Long matchId, Match.MatchStatus status) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
        match.setStatus(status);
        return matchRepository.save(match);
    }
}

