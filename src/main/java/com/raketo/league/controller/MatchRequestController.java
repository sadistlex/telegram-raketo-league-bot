package com.raketo.league.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raketo.league.model.Player;
import com.raketo.league.model.ScheduleRequest;
import com.raketo.league.model.Tour;
import com.raketo.league.repository.PlayerRepository;
import com.raketo.league.repository.ScheduleRequestRepository;
import com.raketo.league.repository.TourRepository;
import com.raketo.league.telegram.TelegramBot;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/match-request")
@RequiredArgsConstructor
public class MatchRequestController {
    private final ScheduleRequestRepository scheduleRequestRepository;
    private final PlayerRepository playerRepository;
    private final TourRepository tourRepository;
    private final TelegramBot telegramBot;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    @Transactional
    public ResponseEntity<List<ScheduleRequest>> createMatchRequests(@RequestBody MatchRequestPayload payload) {
        Player initiator = playerRepository.findById(payload.getPlayerId())
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        Player recipient = playerRepository.findById(payload.getOpponentId())
                .orElseThrow(() -> new IllegalArgumentException("Opponent not found"));
        Tour tour = tourRepository.findById(payload.getTourId())
                .orElseThrow(() -> new IllegalArgumentException("Tour not found"));

        List<ScheduleRequest> createdRequests = new ArrayList<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (DayRequest dayRequest : payload.getRequests()) {
            LocalDate proposedDate = LocalDate.parse(dayRequest.getDay(), dayFormatter);

            try {
                String hoursJson = objectMapper.writeValueAsString(dayRequest.getHours());

                ScheduleRequest scheduleRequest = ScheduleRequest.builder()
                        .tour(tour)
                        .initiatorPlayer(initiator)
                        .recipientPlayer(recipient)
                        .proposedDate(proposedDate)
                        .proposedHours(hoursJson)
                        .status(ScheduleRequest.ScheduleStatus.Pending)
                        .createdAt(LocalDateTime.now())
                        .build();

                createdRequests.add(scheduleRequestRepository.save(scheduleRequest));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize hours", e);
            }
        }

        sendNotifications(initiator, recipient, tour, createdRequests);

        return ResponseEntity.ok(createdRequests);
    }

    private void sendNotifications(Player initiator, Player recipient, Tour tour, List<ScheduleRequest> requests) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        if (initiator.getTelegramId() != null) {
            StringBuilder initiatorMsg = new StringBuilder();
            initiatorMsg.append("✅ Match request sent to ").append(recipient.getName()).append("\n\n");
            initiatorMsg.append("Tour: ").append(tour.getId()).append("\n");
            initiatorMsg.append("Proposed times:\n");
            for (ScheduleRequest req : requests) {
                initiatorMsg.append("📅 ").append(req.getProposedDate().format(dateFormatter));
                try {
                    List<Integer> hours = objectMapper.readValue(req.getProposedHours(), List.class);
                    initiatorMsg.append(" (").append(formatHours(hours)).append(")\n");
                } catch (Exception e) {
                    initiatorMsg.append("\n");
                }
            }

            telegramBot.sendMessage(initiator.getTelegramId(), initiatorMsg.toString());
        }

        if (recipient.getTelegramId() != null) {
            StringBuilder recipientMsg = new StringBuilder();
            recipientMsg.append("🎾 New match request from ").append(initiator.getName()).append("\n\n");
            recipientMsg.append("Tour: ").append(tour.getId()).append("\n");
            recipientMsg.append("Proposed times:\n");
            for (ScheduleRequest req : requests) {
                recipientMsg.append("📅 ").append(req.getProposedDate().format(dateFormatter));
                try {
                    List<Integer> hours = objectMapper.readValue(req.getProposedHours(), List.class);
                    recipientMsg.append(" (").append(formatHours(hours)).append(")\n");
                } catch (Exception e) {
                    recipientMsg.append("\n");
                }
            }
            recipientMsg.append("\nUse /schedule to view and respond to requests.");

            telegramBot.sendMessage(recipient.getTelegramId(), recipientMsg.toString());
        }
    }

    private String formatHours(List<Integer> hours) {
        if (hours.isEmpty()) return "";
        if (hours.size() == 1) return String.format("%02d:00", hours.get(0));
        return String.format("%02d:00-%02d:00", hours.get(0), hours.get(hours.size() - 1));
    }

    @Data
    public static class MatchRequestPayload {
        private Long tourId;
        private Long playerId;
        private Long opponentId;
        private List<DayRequest> requests;
    }

    @Data
    public static class DayRequest {
        private String day;
        private List<Integer> hours;
    }
}

