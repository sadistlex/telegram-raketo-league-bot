package com.raketo.league.service;

import com.raketo.league.model.*;
import com.raketo.league.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final TourPlayerRepository tourPlayerRepository;
    private final PlayerDivisionAssignmentRepository playerDivisionAssignmentRepository;
    private final TourTemplateRepository tourTemplateRepository;
    private final TourRepository tourRepository;
    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Tbilisi");
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM").withZone(ZONE_ID);

    @Transactional(readOnly = true)
    public PlayerSchedule buildPlayerSchedule(Player player) {
        List<PlayerDivisionAssignment> assignments = playerDivisionAssignmentRepository.findByPlayerId(player.getId());
        if (assignments.isEmpty()) { return new PlayerSchedule(player, List.of()); }
        Set<Long> divisionTournamentIds = assignments.stream()
                .filter(a -> a.getDivisionTournament().getTournament().getIsActive())
                .map(a -> a.getDivisionTournament().getId())
                .collect(Collectors.toSet());
        if (divisionTournamentIds.isEmpty()) { return new PlayerSchedule(player, List.of()); }
        return buildPlayerScheduleForDivisions(player, divisionTournamentIds);
    }

    @Transactional(readOnly = true)
    public PlayerSchedule buildPlayerScheduleForDivision(Player player, Long divisionTournamentId) {
        return buildPlayerScheduleForDivisions(player, Set.of(divisionTournamentId));
    }

    @Transactional(readOnly = true)
    public List<PlayerDivisionAssignment> getPlayerDivisions(Player player) {
        return playerDivisionAssignmentRepository.findByPlayerId(player.getId()).stream()
                .filter(a -> a.getDivisionTournament().getTournament().getIsActive())
                .collect(Collectors.toList());
    }

    private PlayerSchedule buildPlayerScheduleForDivisions(Player player, Set<Long> divisionTournamentIds) {
        List<TourTemplate> allTemplates = new ArrayList<>();
        for (Long dtId : divisionTournamentIds) { allTemplates.addAll(tourTemplateRepository.findByDivisionTournamentId(dtId)); }
        allTemplates.sort(Comparator.comparing(TourTemplate::getStartDate));
        List<TourPlayer> tourPlayers = tourPlayerRepository.findByPlayerId(player.getId());
        Map<Long, Tour> playerToursByTemplateId = new HashMap<>();
        Map<Long, Player> opponentsByTemplateId = new HashMap<>();
        for (TourPlayer tp : tourPlayers) {
            Tour tour = tp.getTour();
            Long templateId = tour.getTourTemplate().getId();
            playerToursByTemplateId.put(templateId, tour);
            List<TourPlayer> allPlayersInTour = tourPlayerRepository.findByTourId(tour.getId());
            Player opponent = allPlayersInTour.stream().map(TourPlayer::getPlayer).filter(p -> !Objects.equals(p.getId(), player.getId())).findFirst().orElse(null);
            opponentsByTemplateId.put(templateId, opponent);
        }
        List<TourInfo> tourInfos = new ArrayList<>();
        for (TourTemplate template : allTemplates) {
            Tour tour = playerToursByTemplateId.get(template.getId());
            Player opponent = opponentsByTemplateId.get(template.getId());
            Long tourId = tour != null ? tour.getId() : null;
            Tour.TourStatus status = tour != null ? tour.getStatus() : null;
            LocalDateTime scheduledTime = tour != null ? tour.getScheduledTime() : null;
            Player responsiblePlayer = tour != null ? tour.getResponsiblePlayer() : null;
            Long divisionTournamentId = template.getDivisionTournament().getId();
            tourInfos.add(new TourInfo(tourId, template.getStartDate(), template.getEndDate(), status, opponent, scheduledTime, responsiblePlayer, divisionTournamentId));
        }
        return new PlayerSchedule(player, tourInfos);
    }

    public record PlayerSchedule(Player player, List<TourInfo> tours) {}
    public record TourInfo(Long tourId, LocalDateTime startDate, LocalDateTime endDate, Tour.TourStatus status, Player opponent, LocalDateTime scheduledTime, Player responsiblePlayer, Long divisionTournamentId) {}

    @Transactional(readOnly = true)
    public List<TourRoundInfo> buildTourGroupedSchedule(Long divisionTournamentId) {
        List<TourTemplate> templates = tourTemplateRepository.findByDivisionTournamentId(divisionTournamentId);
        templates.sort(Comparator.comparing(TourTemplate::getStartDate));

        List<TourRoundInfo> result = new ArrayList<>();
        int tourNumber = 1;

        for (TourTemplate template : templates) {
            List<Tour> tours = tourRepository.findByTourTemplateId(template.getId());
            List<MatchPair> matches = new ArrayList<>();

            for (Tour tour : tours) {
                List<TourPlayer> tourPlayers = tourPlayerRepository.findByTourId(tour.getId());
                List<Player> players = tourPlayers.stream().map(TourPlayer::getPlayer).collect(Collectors.toList());
                if (players.size() == 2) {
                    Player responsiblePlayer = tour.getResponsiblePlayer();
                    Player player1, player2;
                    if (responsiblePlayer != null && Objects.equals(players.get(0).getId(), responsiblePlayer.getId())) {
                        player1 = players.get(0);
                        player2 = players.get(1);
                    } else if (responsiblePlayer != null && Objects.equals(players.get(1).getId(), responsiblePlayer.getId())) {
                        player1 = players.get(1);
                        player2 = players.get(0);
                    } else {
                        player1 = players.get(0);
                        player2 = players.get(1);
                    }
                    matches.add(new MatchPair(player1, player2, tour.getStatus(), responsiblePlayer));
                } else if (players.size() == 1) {
                    // BYE
                    matches.add(new MatchPair(players.get(0), null, tour.getStatus(), tour.getResponsiblePlayer()));
                }
            }

            result.add(new TourRoundInfo(tourNumber, template.getStartDate(), template.getEndDate(), matches));
            tourNumber++;
        }

        return result;
    }

    public record TourRoundInfo(int tourNumber, LocalDateTime startDate, LocalDateTime endDate, List<MatchPair> matches) {}
    public record MatchPair(Player player1, Player player2, Tour.TourStatus status, Player responsiblePlayer) {}
}
