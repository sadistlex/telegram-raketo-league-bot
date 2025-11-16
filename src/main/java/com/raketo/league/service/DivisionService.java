package com.raketo.league.service;

import com.raketo.league.model.Division;
import com.raketo.league.model.DivisionTournament;
import com.raketo.league.model.Player;
import com.raketo.league.model.PlayerDivisionAssignment;
import com.raketo.league.repository.DivisionRepository;
import com.raketo.league.repository.DivisionTournamentRepository;
import com.raketo.league.repository.PlayerDivisionAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DivisionService {

    private static final Logger logger = LoggerFactory.getLogger(DivisionService.class);

    private final DivisionRepository divisionRepository;
    private final DivisionTournamentRepository divisionTournamentRepository;
    private final PlayerDivisionAssignmentRepository playerDivisionAssignmentRepository;

    @Transactional(readOnly = true)
    public List<Division> getAllDivisions() {
        return divisionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Division> findById(Long id) {
        return divisionRepository.findById(id);
    }

    @Transactional
    public Division createDivision(String name, Integer level) {
        Division division = Division.builder()
                .name(name)
                .level(level)
                .isActive(true)
                .build();
        return divisionRepository.save(division);
    }

    @Transactional
    public DivisionTournament assignDivisionToTournament(Division division, com.raketo.league.model.Tournament tournament) {
        DivisionTournament dt = DivisionTournament.builder()
                .division(division)
                .tournament(tournament)
                .build();
        return divisionTournamentRepository.save(dt);
    }

    @Transactional
    public PlayerDivisionAssignment assignPlayerToDivision(Player player, DivisionTournament divisionTournament) {
        List<PlayerDivisionAssignment> existing = playerDivisionAssignmentRepository
                .findByPlayerIdAndDivisionTournamentId(player.getId(), divisionTournament.getId());

        if (!existing.isEmpty()) {
            logger.warn("Player {} already assigned to divisionTournament {}", player.getId(), divisionTournament.getId());
            return existing.get(0);
        }

        PlayerDivisionAssignment assignment = PlayerDivisionAssignment.builder()
                .player(player)
                .divisionTournament(divisionTournament)
                .build();
        return playerDivisionAssignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<DivisionTournament> getDivisionTournamentsByTournament(Long tournamentId) {
        return divisionTournamentRepository.findByTournamentId(tournamentId);
    }

    @Transactional(readOnly = true)
    public List<PlayerDivisionAssignment> getPlayersByDivisionTournament(Long divisionTournamentId) {
        return playerDivisionAssignmentRepository.findByDivisionTournamentId(divisionTournamentId);
    }

    @Transactional(readOnly = true)
    public Optional<DivisionTournament> findDivisionTournamentById(Long id) {
        return divisionTournamentRepository.findById(id);
    }
}

