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
    public List<ScheduleRequest> getTourRequests(Long tourId, Long playerId) {
        return scheduleRequestRepository.findByTourIdAndInitiatorPlayerIdOrTourIdAndRecipientPlayerId(tourId, playerId, tourId, playerId);
    }

    public void acceptRequestLocalized(Long requestId, Long acceptingPlayerId, LocalizationService localizationService, BiConsumer<Long, String> messageSender) {
        ScheduleRequest request = scheduleRequestRepository.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!request.getRecipientPlayer().getId().equals(acceptingPlayerId)) {
            throw new IllegalArgumentException(localizationService.msg(request.getRecipientPlayer(), "match.request.accept.only.recipient"));
        }
        if (request.getStatus() != ScheduleRequest.ScheduleStatus.Pending) {
            throw new IllegalArgumentException(localizationService.msg(request.getRecipientPlayer(), "match.request.not.pending"));
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
            Player initiator = request.getInitiatorPlayer();
            String timeStr = "";
            List<Integer> hrs = FormatUtils.parseHoursFromJson(request.getProposedHours());
            if (!hrs.isEmpty()) {
                timeStr = FormatUtils.formatHours(hrs);
            }
            String notification = localizationService.msg(initiator, "match.request.accept.notification", request.getRecipientPlayer().getName(), FormatUtils.formatDate(request.getProposedDate()), timeStr);
            messageSender.accept(initiator.getTelegramId(), notification);
        }
    }

    public void declineRequestLocalized(Long requestId, Long decliningPlayerId, LocalizationService localizationService, BiConsumer<Long, String> messageSender) {
        ScheduleRequest request = scheduleRequestRepository.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!request.getRecipientPlayer().getId().equals(decliningPlayerId)) {
            throw new IllegalArgumentException(localizationService.msg(request.getRecipientPlayer(), "match.request.decline.only.recipient"));
        }
        if (request.getStatus() != ScheduleRequest.ScheduleStatus.Pending) {
            throw new IllegalArgumentException(localizationService.msg(request.getRecipientPlayer(), "match.request.not.pending"));
        }
        request.setStatus(ScheduleRequest.ScheduleStatus.Declined);
        request.setUpdatedAt(LocalDateTime.now());
        scheduleRequestRepository.save(request);
        if (messageSender != null) {
            Player initiator = request.getInitiatorPlayer();
            String notification = localizationService.msg(initiator, "match.request.decline.notification", request.getRecipientPlayer().getName(), FormatUtils.formatDate(request.getProposedDate()));
            messageSender.accept(initiator.getTelegramId(), notification);
        }
    }

    public String formatRequestsMessageLocalized(List<ScheduleRequest> requests, Player currentPlayer, LocalizationService localizationService) {
        if (requests.isEmpty()) {
            return localizationService.msg(currentPlayer, "requests.none");
        }
        StringBuilder sb = new StringBuilder();
        List<ScheduleRequest> incoming = requests.stream().filter(r -> r.getRecipientPlayer().getId().equals(currentPlayer.getId())).toList();
        List<ScheduleRequest> outgoing = requests.stream().filter(r -> r.getInitiatorPlayer().getId().equals(currentPlayer.getId())).toList();
        if (!incoming.isEmpty()) {
            sb.append(localizationService.msg(currentPlayer, "requests.incoming.header"));
            for (ScheduleRequest req : incoming) {
                sb.append(localizationService.msg(currentPlayer, "requests.from", req.getInitiatorPlayer().getName())).append("\n");
                sb.append(localizationService.msg(currentPlayer, "requests.date", FormatUtils.formatDate(req.getProposedDate()))).append("\n");
                List<Integer> hours = FormatUtils.parseHoursFromJson(req.getProposedHours());
                if (!hours.isEmpty()) {
                    sb.append(localizationService.msg(currentPlayer, "requests.time", FormatUtils.formatHours(hours))).append("\n");
                }
                sb.append(localizationService.msg(currentPlayer, "requests.status", getStatusEmoji(req.getStatus()), req.getStatus())).append("\n");
                sb.append(localizationService.msg(currentPlayer, "requests.id", req.getId())).append("\n\n");
            }
        }
        if (!outgoing.isEmpty()) {
            sb.append(localizationService.msg(currentPlayer, "requests.outgoing.header"));
            for (ScheduleRequest req : outgoing) {
                sb.append(localizationService.msg(currentPlayer, "requests.to", req.getRecipientPlayer().getName())).append("\n");
                sb.append(localizationService.msg(currentPlayer, "requests.date", FormatUtils.formatDate(req.getProposedDate()))).append("\n");
                List<Integer> hours = FormatUtils.parseHoursFromJson(req.getProposedHours());
                if (!hours.isEmpty()) {
                    sb.append(localizationService.msg(currentPlayer, "requests.time", FormatUtils.formatHours(hours))).append("\n");
                }
                sb.append(localizationService.msg(currentPlayer, "requests.status", getStatusEmoji(req.getStatus()), req.getStatus())).append("\n\n");
            }
        }
        return sb.toString();
    }

    private String getStatusEmoji(ScheduleRequest.ScheduleStatus status) {
        switch (status) {
            case Pending:
                return "\u23f3";
            case Accepted:
                return "\u2705";
            case Declined:
                return "\u274c";
            case Expired:
                return "\uD83D\uDD50";
            case Cancelled:
                return "\uD83D\uDEAB";
            default:
                return "";
        }
    }
}
