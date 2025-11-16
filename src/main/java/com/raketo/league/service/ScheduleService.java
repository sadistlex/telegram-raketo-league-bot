package com.raketo.league.service;

import com.raketo.league.model.*;
import com.raketo.league.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final TourRepository tourRepository;
    private final TourPlayerRepository tourPlayerRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final PlayerDivisionAssignmentRepository playerDivisionAssignmentRepository;
    private final TourTemplateRepository tourTemplateRepository;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Tbilisi");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM").withZone(ZONE_ID);

    @Transactional(readOnly = true)
    public PlayerSchedule buildPlayerSchedule(Player player) {
        List<PlayerDivisionAssignment> assignments = playerDivisionAssignmentRepository.findByPlayerId(player.getId());
        if (assignments.isEmpty()) {
            return new PlayerSchedule(player, List.of());
        }

        Set<Long> divisionTournamentIds = assignments.stream()
                .map(a -> a.getDivisionTournament().getId())
                .collect(Collectors.toSet());

        List<TourTemplate> allTemplates = new ArrayList<>();
        for (Long dtId : divisionTournamentIds) {
            allTemplates.addAll(tourTemplateRepository.findByDivisionTournamentId(dtId));
        }

        allTemplates.sort(Comparator.comparing(TourTemplate::getStartDate));

        List<TourPlayer> tourPlayers = tourPlayerRepository.findByPlayerId(player.getId());
        Map<Long, Tour> playerToursByTemplateId = new HashMap<>();
        Map<Long, Player> opponentsByTemplateId = new HashMap<>();

        for (TourPlayer tp : tourPlayers) {
            Tour tour = tp.getTour();
            Long templateId = tour.getTourTemplate().getId();
            playerToursByTemplateId.put(templateId, tour);

            List<TourPlayer> allPlayersInTour = tourPlayerRepository.findByTourId(tour.getId());
            Player opponent = allPlayersInTour.stream()
                    .map(TourPlayer::getPlayer)
                    .filter(p -> !Objects.equals(p.getId(), player.getId()))
                    .findFirst()
                    .orElse(null);
            opponentsByTemplateId.put(templateId, opponent);
        }

        List<TourInfo> tourInfos = new ArrayList<>();
        for (TourTemplate template : allTemplates) {
            Tour tour = playerToursByTemplateId.get(template.getId());
            Player opponent = opponentsByTemplateId.get(template.getId());

            Long tourId = tour != null ? tour.getId() : null;
            Tour.TourStatus status = tour != null ? tour.getStatus() : null;

            tourInfos.add(new TourInfo(tourId, template.getStartDate(), template.getEndDate(), status, opponent));
        }

        return new PlayerSchedule(player, tourInfos);
    }

    public String renderScheduleMessage(PlayerSchedule schedule) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(schedule.player().getName()).append(" (@").append(schedule.player().getTelegramUsername()).append(")\n\n");
        if (schedule.tours().isEmpty()) {
            sb.append("No tours assigned yet.");
        } else {
            int tourNumber = 1;
            for (TourInfo ti : schedule.tours()) {
                sb.append("Tour ").append(tourNumber).append(" (")
                        .append(DATE_FMT.format(ti.startDate())).append("-")
                        .append(DATE_FMT.format(ti.endDate())).append(") - ");

                if (ti.opponent() != null) {
                    List<AvailabilitySlot> availabilitySlots = availabilitySlotRepository.findByPlayerIdAndTourId(
                            schedule.player().getId(), ti.tourId());
                    String availabilityStatus = availabilitySlots.isEmpty() ? "Not Set" : "Set";

                    sb.append(ti.opponent().getName())
                            .append(", Status: ").append(ti.status())
                            .append(", Availability: ").append(availabilityStatus);
                } else {
                    sb.append("Bye");
                }

                sb.append("\n");
                tourNumber++;
            }
        }
        return sb.toString();
    }

    public record PlayerSchedule(Player player, List<TourInfo> tours) {}
    public record TourInfo(Long tourId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Tour.TourStatus status, Player opponent) {}
}
