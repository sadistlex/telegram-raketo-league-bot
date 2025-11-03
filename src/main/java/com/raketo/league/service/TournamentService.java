package com.raketo.league.service;

import com.raketo.league.model.Tournament;
import com.raketo.league.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentService {

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

