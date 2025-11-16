package com.raketo.league.service;

import com.raketo.league.model.*;
import com.raketo.league.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourService {

    private static final Logger logger = LoggerFactory.getLogger(TourService.class);

    private final TourTemplateRepository tourTemplateRepository;
    private final TourRepository tourRepository;
    private final TourPlayerRepository tourPlayerRepository;
    private final PlayerDivisionAssignmentRepository playerDivisionAssignmentRepository;
    private final DivisionTournamentRepository divisionTournamentRepository;

    @Transactional
    public int generateRoundRobinTours(Long divisionTournamentId, LocalDateTime tournamentStartDate, int tourDurationDays) {
        logger.info("Generating round-robin tours for divisionTournamentId: {}, startDate: {}, durationDays: {}",
                divisionTournamentId, tournamentStartDate, tourDurationDays);

        DivisionTournament divisionTournament = divisionTournamentRepository.findById(divisionTournamentId)
                .orElseThrow(() -> new IllegalArgumentException("DivisionTournament not found with id: " + divisionTournamentId));

        List<PlayerDivisionAssignment> playerAssignments = playerDivisionAssignmentRepository.findByDivisionTournamentId(divisionTournamentId);
        if (playerAssignments.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players to generate tours");
        }

        List<Player> players = playerAssignments.stream()
                .map(PlayerDivisionAssignment::getPlayer)
                .toList();

        int numberOfTours = players.size() - 1;

        List<TourTemplate> existingTemplates = tourTemplateRepository.findByDivisionTournamentId(divisionTournamentId);
        if (!existingTemplates.isEmpty()) {
            logger.warn("Tour templates already exist for divisionTournamentId: {}. Deleting existing templates and tours.", divisionTournamentId);
            List<Tour> existingTours = existingTemplates.stream()
                    .flatMap(template -> tourRepository.findByTourTemplateId(template.getId()).stream())
                    .toList();
            for (Tour tour : existingTours) {
                tourPlayerRepository.deleteAll(tourPlayerRepository.findByTourId(tour.getId()));
            }
            tourRepository.deleteAll(existingTours);
            tourTemplateRepository.deleteAll(existingTemplates);
        }

        List<TourTemplate> tourTemplates = new ArrayList<>();
        LocalDateTime currentStartDate = tournamentStartDate;

        for (int i = 0; i < numberOfTours; i++) {
            LocalDateTime endDate = currentStartDate.plusDays(tourDurationDays);

            TourTemplate template = TourTemplate.builder()
                    .divisionTournament(divisionTournament)
                    .startDate(currentStartDate)
                    .endDate(endDate)
                    .build();

            tourTemplates.add(template);
            currentStartDate = endDate;
        }

        tourTemplateRepository.saveAll(tourTemplates);

        List<List<PlayerPair>> roundRobinSchedule = generateRoundRobinSchedule(players);

        if (roundRobinSchedule.size() != tourTemplates.size()) {
            throw new IllegalStateException("Mismatch between tour templates count and generated rounds. Templates: " +
                    tourTemplates.size() + ", Rounds: " + roundRobinSchedule.size());
        }

        int totalToursCreated = 0;
        for (int i = 0; i < roundRobinSchedule.size(); i++) {
            TourTemplate template = tourTemplates.get(i);
            List<PlayerPair> pairs = roundRobinSchedule.get(i);

            for (PlayerPair pair : pairs) {
                Tour tour = Tour.builder()
                        .tourTemplate(template)
                        .status(Tour.TourStatus.Active)
                        .updatedAt(LocalDateTime.now())
                        .build();

                Tour savedTour = tourRepository.save(tour);

                TourPlayer tourPlayer1 = TourPlayer.builder()
                        .tour(savedTour)
                        .player(pair.player1)
                        .build();

                TourPlayer tourPlayer2 = TourPlayer.builder()
                        .tour(savedTour)
                        .player(pair.player2)
                        .build();

                tourPlayerRepository.save(tourPlayer1);
                tourPlayerRepository.save(tourPlayer2);
                totalToursCreated++;
            }
        }

        logger.info("Generated {} tour templates and {} tour pairings for divisionTournamentId: {}",
                numberOfTours, totalToursCreated, divisionTournamentId);

        return totalToursCreated;
    }

    @Transactional
    public void generateTourTemplates(Long divisionTournamentId, LocalDateTime tournamentStartDate, int tourDurationWeeks) {
        logger.info("Generating tour templates for divisionTournamentId: {}", divisionTournamentId);

        DivisionTournament divisionTournament = divisionTournamentRepository.findById(divisionTournamentId)
                .orElseThrow(() -> new IllegalArgumentException("DivisionTournament not found with id: " + divisionTournamentId));

        List<PlayerDivisionAssignment> players = playerDivisionAssignmentRepository.findByDivisionTournamentId(divisionTournamentId);
        int numberOfPlayers = players.size();

        if (numberOfPlayers < 2) {
            throw new IllegalArgumentException("Need at least 2 players to generate tour templates");
        }

        int numberOfTours = numberOfPlayers - 1;

        List<TourTemplate> existingTemplates = tourTemplateRepository.findByDivisionTournamentId(divisionTournamentId);
        if (!existingTemplates.isEmpty()) {
            logger.warn("Tour templates already exist for divisionTournamentId: {}. Deleting existing templates.", divisionTournamentId);
            tourTemplateRepository.deleteAll(existingTemplates);
        }

        List<TourTemplate> tourTemplates = new ArrayList<>();
        LocalDateTime currentStartDate = tournamentStartDate;

        for (int i = 0; i < numberOfTours; i++) {
            LocalDateTime endDate = currentStartDate.plusWeeks(tourDurationWeeks);

            TourTemplate template = TourTemplate.builder()
                    .divisionTournament(divisionTournament)
                    .startDate(currentStartDate)
                    .endDate(endDate)
                    .build();

            tourTemplates.add(template);
            currentStartDate = endDate;
        }

        tourTemplateRepository.saveAll(tourTemplates);
        logger.info("Generated {} tour templates for divisionTournamentId: {}", numberOfTours, divisionTournamentId);
    }

    @Transactional
    public void generateTourPairings(Long divisionTournamentId) {
        logger.info("Generating tour pairings for divisionTournamentId: {}", divisionTournamentId);

        List<TourTemplate> tourTemplates = tourTemplateRepository.findByDivisionTournamentId(divisionTournamentId);
        if (tourTemplates.isEmpty()) {
            throw new IllegalArgumentException("No tour templates found for divisionTournamentId: " + divisionTournamentId + ". Generate templates first.");
        }

        List<PlayerDivisionAssignment> playerAssignments = playerDivisionAssignmentRepository.findByDivisionTournamentId(divisionTournamentId);
        if (playerAssignments.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players to generate pairings");
        }

        List<Player> players = playerAssignments.stream()
                .map(PlayerDivisionAssignment::getPlayer)
                .toList();

        List<List<PlayerPair>> roundRobinSchedule = generateRoundRobinSchedule(players);

        if (roundRobinSchedule.size() != tourTemplates.size()) {
            throw new IllegalStateException("Mismatch between tour templates count and generated rounds. Templates: " +
                    tourTemplates.size() + ", Rounds: " + roundRobinSchedule.size());
        }

        for (int i = 0; i < roundRobinSchedule.size(); i++) {
            TourTemplate template = tourTemplates.get(i);
            List<PlayerPair> pairs = roundRobinSchedule.get(i);

            for (PlayerPair pair : pairs) {
                Tour tour = Tour.builder()
                        .tourTemplate(template)
                        .status(Tour.TourStatus.Active)
                        .updatedAt(LocalDateTime.now())
                        .build();

                Tour savedTour = tourRepository.save(tour);

                TourPlayer tourPlayer1 = TourPlayer.builder()
                        .tour(savedTour)
                        .player(pair.player1)
                        .build();

                TourPlayer tourPlayer2 = TourPlayer.builder()
                        .tour(savedTour)
                        .player(pair.player2)
                        .build();

                tourPlayerRepository.save(tourPlayer1);
                tourPlayerRepository.save(tourPlayer2);
            }
        }

        logger.info("Generated tour pairings for {} tours in divisionTournamentId: {}", roundRobinSchedule.size(), divisionTournamentId);
    }

    private List<List<PlayerPair>> generateRoundRobinSchedule(List<Player> players) {
        int n = players.size();
        boolean isOdd = n % 2 == 1;

        List<Player> playerList = new ArrayList<>(players);
        if (isOdd) {
            playerList.add(null);
            n++;
        }

        List<List<PlayerPair>> schedule = new ArrayList<>();

        for (int round = 0; round < n - 1; round++) {
            List<PlayerPair> roundPairs = new ArrayList<>();

            for (int i = 0; i < n / 2; i++) {
                int home = i;
                int away = n - 1 - i;

                Player player1 = playerList.get(home);
                Player player2 = playerList.get(away);

                if (player1 != null && player2 != null) {
                    roundPairs.add(new PlayerPair(player1, player2));
                }
            }

            schedule.add(roundPairs);

            Player fixed = playerList.get(0);
            playerList.remove(0);
            Collections.rotate(playerList, 1);
            playerList.add(0, fixed);
        }

        return schedule;
    }

    private static class PlayerPair {
        Player player1;
        Player player2;

        PlayerPair(Player player1, Player player2) {
            this.player1 = player1;
            this.player2 = player2;
        }
    }
}
