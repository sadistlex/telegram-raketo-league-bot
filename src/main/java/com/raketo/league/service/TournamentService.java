package com.raketo.league.service;

import com.raketo.league.model.Tournament;
import com.raketo.league.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private static final Logger logger = LoggerFactory.getLogger(TournamentService.class);

    private final TournamentRepository tournamentRepository;

    @Transactional(readOnly = true)
    public List<Tournament> getActiveTournaments() {
        return tournamentRepository.findByStatus(Tournament.TournamentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<Tournament> findById(Long id) {
        return tournamentRepository.findById(id);
    }

    @Transactional
    public Tournament saveTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }
}

