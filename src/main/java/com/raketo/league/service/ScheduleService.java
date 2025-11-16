package com.raketo.league.service;

import com.raketo.league.model.Player;
import com.raketo.league.model.Tour;
import com.raketo.league.model.TourPlayer;
import com.raketo.league.repository.TourPlayerRepository;
import com.raketo.league.repository.TourRepository;
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
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Tbilisi");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM HH.mm").withZone(ZONE_ID);

    @Transactional(readOnly = true)
    public PlayerSchedule buildPlayerSchedule(Player player) {
        List<TourPlayer> tourPlayers = tourPlayerRepository.findByPlayerId(player.getId());
        Map<Long, List<TourPlayer>> byTour = tourPlayers.stream().collect(Collectors.groupingBy(tp -> tp.getTour().getId()));
        List<TourInfo> tourInfos = new ArrayList<>();
        for (Long tourId : byTour.keySet()) {
            Tour tour = tourRepository.findById(tourId).orElse(null);
            if (tour == null) continue;
            List<TourPlayer> players = byTour.get(tourId);
            Player opponent = players.stream().map(TourPlayer::getPlayer).filter(p -> !Objects.equals(p.getId(), player.getId())).findFirst().orElse(null);
            tourInfos.add(new TourInfo(tour.getId(), tour.getTourTemplate().getStartDate(), tour.getTourTemplate().getEndDate(), tour.getStatus(), opponent));
        }
        tourInfos.sort(Comparator.comparing(TourInfo::startDate));
        return new PlayerSchedule(player, tourInfos);
    }

    public String renderScheduleMessage(PlayerSchedule schedule) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(schedule.player().getName()).append(" (@").append(schedule.player().getTelegramUsername()).append(")\n\n");
        if (schedule.tours().isEmpty()) sb.append("No tours assigned yet.");
        for (TourInfo ti : schedule.tours()) {
            sb.append("Tour ").append(ti.tourId()).append(" (")
                    .append(DATE_FMT.format(ti.startDate())).append("-")
                    .append(DATE_FMT.format(ti.endDate())).append(") Status: ").append(ti.status()).append("\n");
            if (ti.opponent() != null) sb.append("Opponent: ").append(ti.opponent().getName()).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    public record PlayerSchedule(Player player, List<TourInfo> tours) {}
    public record TourInfo(Long tourId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Tour.TourStatus status, Player opponent) {}
}
