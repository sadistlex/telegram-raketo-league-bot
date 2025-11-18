package com.raketo.league.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raketo.league.model.Player;
import com.raketo.league.model.ScheduleRequest;
import com.raketo.league.model.Tour;
import com.raketo.league.repository.ScheduleRequestRepository;
import com.raketo.league.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class ScheduleRequestService {
    private final ScheduleRequestRepository scheduleRequestRepository;
    private final TourRepository tourRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<ScheduleRequest> getPlayerRequests(Long playerId) {
        return scheduleRequestRepository.findByInitiatorPlayerIdOrRecipientPlayerId(playerId, playerId);
    }

    @Transactional(readOnly = true)
    public List<ScheduleRequest> getTourRequests(Long tourId, Long playerId) {
        return scheduleRequestRepository.findByTourIdAndInitiatorPlayerIdOrTourIdAndRecipientPlayerId(
            tourId, playerId, tourId, playerId);
    }

    @Transactional
    public void acceptRequest(Long requestId, Long acceptingPlayerId, BiConsumer<Long, String> messageSender) {
        ScheduleRequest request = scheduleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!request.getRecipientPlayer().getId().equals(acceptingPlayerId)) {
            throw new IllegalArgumentException("Only recipient can accept request");
        }

        if (request.getStatus() != ScheduleRequest.ScheduleStatus.Pending) {
            throw new IllegalArgumentException("Request is not pending");
        }

        request.setStatus(ScheduleRequest.ScheduleStatus.Accepted);
        request.setUpdatedAt(LocalDateTime.now());
        scheduleRequestRepository.save(request);

        Tour tour = request.getTour();

        try {
            List<Integer> hours = objectMapper.readValue(request.getProposedHours(), List.class);
            if (!hours.isEmpty()) {
                LocalDateTime scheduledTime = request.getProposedDate().atTime(hours.get(0), 0);
                tour.setScheduledTime(scheduledTime);
            }
        } catch (Exception e) {
        }

        tour.setStatus(Tour.TourStatus.Scheduled);
        tour.setUpdatedAt(LocalDateTime.now());
        tourRepository.save(tour);

        if (messageSender != null) {
            String notification = buildAcceptanceNotification(request);
            messageSender.accept(request.getInitiatorPlayer().getTelegramId(), notification);
        }
    }

    @Transactional
    public void declineRequest(Long requestId, Long decliningPlayerId, BiConsumer<Long, String> messageSender) {
        ScheduleRequest request = scheduleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!request.getRecipientPlayer().getId().equals(decliningPlayerId)) {
            throw new IllegalArgumentException("Only recipient can decline request");
        }

        if (request.getStatus() != ScheduleRequest.ScheduleStatus.Pending) {
            throw new IllegalArgumentException("Request is not pending");
        }

        request.setStatus(ScheduleRequest.ScheduleStatus.Declined);
        request.setUpdatedAt(LocalDateTime.now());
        scheduleRequestRepository.save(request);

        if (messageSender != null) {
            String notification = buildDeclineNotification(request);
            messageSender.accept(request.getInitiatorPlayer().getTelegramId(), notification);
        }
    }

    private String buildAcceptanceNotification(ScheduleRequest request) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        StringBuilder msg = new StringBuilder();
        msg.append("✅ Your match request was accepted!\n\n");
        msg.append("Opponent: ").append(request.getRecipientPlayer().getName()).append("\n");
        msg.append("Date: ").append(request.getProposedDate().format(dateFormatter)).append("\n");

        try {
            List<Integer> hours = objectMapper.readValue(request.getProposedHours(), List.class);
            if (!hours.isEmpty()) {
                msg.append("Time: ").append(formatHours(hours)).append("\n");
            }
        } catch (Exception e) {
        }

        return msg.toString();
    }

    private String buildDeclineNotification(ScheduleRequest request) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        StringBuilder msg = new StringBuilder();
        msg.append("❌ Your match request was declined.\n\n");
        msg.append("Opponent: ").append(request.getRecipientPlayer().getName()).append("\n");
        msg.append("Date: ").append(request.getProposedDate().format(dateFormatter)).append("\n");

        return msg.toString();
    }

    private String formatHours(List<Integer> hours) {
        if (hours.isEmpty()) return "";
        if (hours.size() == 1) return String.format("%02d:00", hours.get(0));
        return String.format("%02d:00-%02d:00", hours.get(0), hours.get(hours.size() - 1));
    }

    public String formatRequestsMessage(List<ScheduleRequest> requests, Player currentPlayer) {
        if (requests.isEmpty()) {
            return "No match requests.";
        }

        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        List<ScheduleRequest> incoming = requests.stream()
                .filter(r -> r.getRecipientPlayer().getId().equals(currentPlayer.getId()))
                .toList();
        List<ScheduleRequest> outgoing = requests.stream()
                .filter(r -> r.getInitiatorPlayer().getId().equals(currentPlayer.getId()))
                .toList();

        if (!incoming.isEmpty()) {
            sb.append("📥 Incoming Requests:\n\n");
            for (ScheduleRequest req : incoming) {
                sb.append("From: ").append(req.getInitiatorPlayer().getName()).append("\n");
                sb.append("Date: ").append(req.getProposedDate().format(dateFormatter)).append("\n");
                try {
                    List<Integer> hours = objectMapper.readValue(req.getProposedHours(), List.class);
                    sb.append("Time: ").append(formatHours(hours)).append("\n");
                } catch (Exception e) {
                }
                sb.append("Status: ").append(getStatusEmoji(req.getStatus())).append(" ").append(req.getStatus()).append("\n");
                sb.append("ID: ").append(req.getId()).append("\n\n");
            }
        }

        if (!outgoing.isEmpty()) {
            sb.append("📤 Outgoing Requests:\n\n");
            for (ScheduleRequest req : outgoing) {
                sb.append("To: ").append(req.getRecipientPlayer().getName()).append("\n");
                sb.append("Date: ").append(req.getProposedDate().format(dateFormatter)).append("\n");
                try {
                    List<Integer> hours = objectMapper.readValue(req.getProposedHours(), List.class);
                    sb.append("Time: ").append(formatHours(hours)).append("\n");
                } catch (Exception e) {
                }
                sb.append("Status: ").append(getStatusEmoji(req.getStatus())).append(" ").append(req.getStatus()).append("\n\n");
            }
        }

        return sb.toString();
    }

    private String getStatusEmoji(ScheduleRequest.ScheduleStatus status) {
        return switch (status) {
            case Pending -> "⏳";
            case Accepted -> "✅";
            case Declined -> "❌";
            case Expired -> "🕐";
            case Cancelled -> "🚫";
        };
    }
}

