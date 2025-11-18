package com.raketo.league.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raketo.league.model.Player;
import com.raketo.league.model.ScheduleRequest;
import com.raketo.league.model.Tour;
import com.raketo.league.repository.PlayerRepository;
import com.raketo.league.repository.ScheduleRequestRepository;
import com.raketo.league.repository.TourRepository;
import com.raketo.league.telegram.TelegramBot;
import com.raketo.league.util.FormatUtils;
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

            String hoursJson = FormatUtils.hoursToJson(dayRequest.getHours());

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
        }

        sendNotifications(initiator, recipient, tour, createdRequests);

        return ResponseEntity.ok(createdRequests);
    }

    private void sendNotifications(Player initiator, Player recipient, Tour tour, List<ScheduleRequest> requests) {
        if (initiator.getTelegramId() != null) {
            StringBuilder initiatorMsg = new StringBuilder();
            initiatorMsg.append("✅ Match request sent to ").append(recipient.getName()).append("\n\n");
            initiatorMsg.append("Tour: ").append(tour.getId()).append("\n");
            initiatorMsg.append("Proposed times:\n");
            for (ScheduleRequest req : requests) {
                initiatorMsg.append("📅 ").append(FormatUtils.formatDate(req.getProposedDate()));
                List<Integer> hours = FormatUtils.parseHoursFromJson(req.getProposedHours());
                if (!hours.isEmpty()) {
                    initiatorMsg.append(" (").append(FormatUtils.formatHours(hours)).append(")");
                }
                initiatorMsg.append("\n");
            }

            telegramBot.sendMessage(initiator.getTelegramId(), initiatorMsg.toString());
        }

        if (recipient.getTelegramId() != null) {
            StringBuilder recipientMsg = new StringBuilder();
            recipientMsg.append("🎾 New match request from ").append(initiator.getName()).append("\n\n");
            recipientMsg.append("Tour: ").append(tour.getId()).append("\n");
            recipientMsg.append("Proposed times:\n");
            for (ScheduleRequest req : requests) {
                recipientMsg.append("📅 ").append(FormatUtils.formatDate(req.getProposedDate()));
                List<Integer> hours = FormatUtils.parseHoursFromJson(req.getProposedHours());
                if (!hours.isEmpty()) {
                    recipientMsg.append(" (").append(FormatUtils.formatHours(hours)).append(")");
                }
                recipientMsg.append("\n");
            }
            recipientMsg.append("\nUse /schedule to view and respond to requests.");

            telegramBot.sendMessage(recipient.getTelegramId(), recipientMsg.toString());
        }
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

