package com.raketo.league.controller;

import com.raketo.league.model.Player;
import com.raketo.league.model.ScheduleRequest;
import com.raketo.league.model.Tour;
import com.raketo.league.repository.PlayerRepository;
import com.raketo.league.repository.ScheduleRequestRepository;
import com.raketo.league.repository.TourRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

        for (DayRequest dayRequest : payload.getRequests()) {
            for (Integer hour : dayRequest.getHours()) {
                LocalDateTime proposedTime = LocalDateTime.parse(
                    dayRequest.getDay() + " " + String.format("%02d:00:00", hour),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
                );

                ScheduleRequest scheduleRequest = ScheduleRequest.builder()
                        .tour(tour)
                        .initiatorPlayer(initiator)
                        .recipientPlayer(recipient)
                        .proposedTime(proposedTime)
                        .status(ScheduleRequest.ScheduleStatus.Pending)
                        .createdAt(LocalDateTime.now())
                        .build();

                createdRequests.add(scheduleRequestRepository.save(scheduleRequest));
            }
        }

        return ResponseEntity.ok(createdRequests);
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

