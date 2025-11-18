package com.raketo.league.service;

import com.raketo.league.model.Player;
import com.raketo.league.model.ScheduleRequest;
import com.raketo.league.model.Tour;
import com.raketo.league.repository.ScheduleRequestRepository;
import com.raketo.league.repository.TourRepository;
import com.raketo.league.util.FormatUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class ScheduleRequestService {
    private final ScheduleRequestRepository scheduleRequestRepository;
    private final TourRepository tourRepository;

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

        List<Integer> hours = FormatUtils.parseHoursFromJson(request.getProposedHours());
        if (!hours.isEmpty()) {
            LocalDateTime scheduledTime = request.getProposedDate().atTime(hours.get(0), 0);
            tour.setScheduledTime(scheduledTime);
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
        StringBuilder msg = new StringBuilder();
        msg.append("✅ Your match request was accepted!\n\n");
        msg.append("Opponent: ").append(request.getRecipientPlayer().getName()).append("\n");
        msg.append("Date: ").append(FormatUtils.formatDate(request.getProposedDate())).append("\n");

        List<Integer> hours = FormatUtils.parseHoursFromJson(request.getProposedHours());
        if (!hours.isEmpty()) {
            msg.append("Time: ").append(FormatUtils.formatHours(hours)).append("\n");
        }

        return msg.toString();
    }

    private String buildDeclineNotification(ScheduleRequest request) {
        StringBuilder msg = new StringBuilder();
        msg.append("❌ Your match request was declined.\n\n");
        msg.append("Opponent: ").append(request.getRecipientPlayer().getName()).append("\n");
        msg.append("Date: ").append(FormatUtils.formatDate(request.getProposedDate())).append("\n");

        return msg.toString();
    }


    public String formatRequestsMessage(List<ScheduleRequest> requests, Player currentPlayer) {
        if (requests.isEmpty()) {
            return "No match requests.";
        }

        StringBuilder sb = new StringBuilder();

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
                sb.append("Date: ").append(FormatUtils.formatDate(req.getProposedDate())).append("\n");
                List<Integer> hours = FormatUtils.parseHoursFromJson(req.getProposedHours());
                if (!hours.isEmpty()) {
                    sb.append("Time: ").append(FormatUtils.formatHours(hours)).append("\n");
                }
                sb.append("Status: ").append(getStatusEmoji(req.getStatus())).append(" ").append(req.getStatus()).append("\n");
                sb.append("ID: ").append(req.getId()).append("\n\n");
            }
        }

        if (!outgoing.isEmpty()) {
            sb.append("📤 Outgoing Requests:\n\n");
            for (ScheduleRequest req : outgoing) {
                sb.append("To: ").append(req.getRecipientPlayer().getName()).append("\n");
                sb.append("Date: ").append(FormatUtils.formatDate(req.getProposedDate())).append("\n");
                List<Integer> hours = FormatUtils.parseHoursFromJson(req.getProposedHours());
                if (!hours.isEmpty()) {
                    sb.append("Time: ").append(FormatUtils.formatHours(hours)).append("\n");
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

