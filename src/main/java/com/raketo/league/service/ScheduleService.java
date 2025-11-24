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
    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Tbilisi");
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM").withZone(ZONE_ID);

    @Transactional(readOnly = true)
    public PlayerSchedule buildPlayerSchedule(Player player) {
        List<PlayerDivisionAssignment> assignments = playerDivisionAssignmentRepository.findByPlayerId(player.getId());
        if (assignments.isEmpty()) { return new PlayerSchedule(player, List.of()); }
        Set<Long> divisionTournamentIds = assignments.stream().map(a -> a.getDivisionTournament().getId()).collect(Collectors.toSet());
        return buildPlayerScheduleForDivisions(player, divisionTournamentIds);
    }

    @Transactional(readOnly = true)
    public PlayerSchedule buildPlayerScheduleForDivision(Player player, Long divisionTournamentId) {
        return buildPlayerScheduleForDivisions(player, Set.of(divisionTournamentId));
    }

    @Transactional(readOnly = true)
    public List<PlayerDivisionAssignment> getPlayerDivisions(Player player) {
        return playerDivisionAssignmentRepository.findByPlayerId(player.getId());
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
}
