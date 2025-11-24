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
                sb.append(localizationService.msg(currentPlayer, "requests.date", FormatUtils.formatDateWithDay(req.getProposedDate()))).append("\n");
                List<Integer> hours = FormatUtils.parseHoursFromJson(req.getProposedHours());
                if (!hours.isEmpty()) {
                    sb.append(localizationService.msg(currentPlayer, "requests.time", FormatUtils.formatHours(hours))).append("\n");
                }
                sb.append(localizationService.msg(currentPlayer, "requests.status", getStatusEmoji(req.getStatus()), req.getStatus())).append("\n\n");
            }
        }
        if (!outgoing.isEmpty()) {
            sb.append(localizationService.msg(currentPlayer, "requests.outgoing.header"));
            for (ScheduleRequest req : outgoing) {
                sb.append(localizationService.msg(currentPlayer, "requests.to", req.getRecipientPlayer().getName())).append("\n");
                sb.append(localizationService.msg(currentPlayer, "requests.date", FormatUtils.formatDateWithDay(req.getProposedDate()))).append("\n");
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
            case Booked:
                return "🎾";
            default:
                return "";
        }
    }

    @Transactional
    public void completeTour(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found"));

        tour.setStatus(Tour.TourStatus.Completed);
        tour.setCompleteDate(LocalDateTime.now());
        tour.setUpdatedAt(LocalDateTime.now());
        tourRepository.save(tour);

        List<ScheduleRequest> pendingRequests = scheduleRequestRepository.findByTourIdAndStatus(tourId, ScheduleRequest.ScheduleStatus.Pending);
        for (ScheduleRequest req : pendingRequests) {
            req.setStatus(ScheduleRequest.ScheduleStatus.Cancelled);
            req.setUpdatedAt(LocalDateTime.now());
            scheduleRequestRepository.save(req);
        }
    }

    @Transactional
    public void postponeTour(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found"));

        tour.setStatus(Tour.TourStatus.Postponed);
        tour.setScheduledTime(null);
        tour.setUpdatedAt(LocalDateTime.now());
        tourRepository.save(tour);

        List<ScheduleRequest> acceptedRequests = scheduleRequestRepository.findByTourIdAndStatus(tourId, ScheduleRequest.ScheduleStatus.Accepted);
        for (ScheduleRequest req : acceptedRequests) {
            req.setStatus(ScheduleRequest.ScheduleStatus.Cancelled);
            req.setUpdatedAt(LocalDateTime.now());
            scheduleRequestRepository.save(req);
        }
    }

    @Transactional
    public void cancelRequestLocalized(Long requestId, Long cancellingPlayerId, LocalizationService localizationService, BiConsumer<Long, String> messageSender) {
        ScheduleRequest request = scheduleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!request.getInitiatorPlayer().getId().equals(cancellingPlayerId)) {
            throw new IllegalArgumentException(localizationService.msg(request.getInitiatorPlayer(), "match.request.cancel.only.initiator"));
        }

        ScheduleRequest.ScheduleStatus oldStatus = request.getStatus();
        request.setStatus(ScheduleRequest.ScheduleStatus.Cancelled);
        request.setUpdatedAt(LocalDateTime.now());
        scheduleRequestRepository.save(request);

        if (oldStatus == ScheduleRequest.ScheduleStatus.Accepted) {
            Tour tour = request.getTour();
            tour.setStatus(Tour.TourStatus.Active);
            tour.setScheduledTime(null);
            tour.setUpdatedAt(LocalDateTime.now());
            tourRepository.save(tour);
        }

        if (messageSender != null && oldStatus != ScheduleRequest.ScheduleStatus.Pending) {
            Player recipient = request.getRecipientPlayer();
            String notification = localizationService.msg(recipient, "match.request.cancel.notification",
                request.getInitiatorPlayer().getName(),
                FormatUtils.formatDate(request.getProposedDate()));
            messageSender.accept(recipient.getTelegramId(), notification);
        }
    }

    @Transactional
    public void changeRequestStatusLocalized(Long requestId, Long requestingPlayerId, ScheduleRequest.ScheduleStatus newStatus,
                                            LocalizationService localizationService, BiConsumer<Long, String> messageSender) {
        ScheduleRequest request = scheduleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!request.getRecipientPlayer().getId().equals(requestingPlayerId)) {
            throw new IllegalArgumentException(localizationService.msg(request.getRecipientPlayer(), "match.request.change.only.recipient"));
        }

        ScheduleRequest.ScheduleStatus oldStatus = request.getStatus();

        if (oldStatus == ScheduleRequest.ScheduleStatus.Pending) {
            throw new IllegalArgumentException(localizationService.msg(request.getRecipientPlayer(), "match.request.change.use.accept.decline"));
        }

        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDateTime.now());
        scheduleRequestRepository.save(request);

        Tour tour = request.getTour();

        if (newStatus == ScheduleRequest.ScheduleStatus.Accepted) {
            List<Integer> hours = FormatUtils.parseHoursFromJson(request.getProposedHours());
            if (!hours.isEmpty()) {
                LocalDateTime scheduledTime = request.getProposedDate().atTime(hours.get(0), 0);
                tour.setScheduledTime(scheduledTime);
            }
            tour.setStatus(Tour.TourStatus.Scheduled);
        } else if (oldStatus == ScheduleRequest.ScheduleStatus.Accepted) {
            tour.setStatus(Tour.TourStatus.Active);
            tour.setScheduledTime(null);
        }
        tour.setUpdatedAt(LocalDateTime.now());
        tourRepository.save(tour);

        if (messageSender != null) {
            Player initiator = request.getInitiatorPlayer();
            String notification;
            if (newStatus == ScheduleRequest.ScheduleStatus.Accepted) {
                List<Integer> hrs = FormatUtils.parseHoursFromJson(request.getProposedHours());
                String timeStr = hrs.isEmpty() ? "" : FormatUtils.formatHours(hrs);
                notification = localizationService.msg(initiator, "match.request.accept.notification",
                    request.getRecipientPlayer().getName(),
                    FormatUtils.formatDate(request.getProposedDate()),
                    timeStr);
            } else {
                notification = localizationService.msg(initiator, "match.request.decline.notification",
                    request.getRecipientPlayer().getName(),
                    FormatUtils.formatDate(request.getProposedDate()));
            }
            messageSender.accept(initiator.getTelegramId(), notification);
        }
    }

    @Transactional
    public void bookRequestLocalized(Long requestId, Long bookingPlayerId, LocalizationService localizationService, BiConsumer<Long, String> messageSender) {
        ScheduleRequest request = scheduleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getStatus() != ScheduleRequest.ScheduleStatus.Accepted) {
            throw new IllegalArgumentException(localizationService.msg(null, "match.request.book.only.accepted"));
        }

        Tour tour = request.getTour();
        Player responsiblePlayer = tour.getResponsiblePlayer();

        if (responsiblePlayer == null || !responsiblePlayer.getId().equals(bookingPlayerId)) {
            throw new IllegalArgumentException(localizationService.msg(null, "match.request.book.only.responsible"));
        }

        request.setStatus(ScheduleRequest.ScheduleStatus.Booked);
        request.setUpdatedAt(LocalDateTime.now());
        scheduleRequestRepository.save(request);

        if (messageSender != null) {
            Player opponent = request.getInitiatorPlayer().getId().equals(bookingPlayerId) ?
                request.getRecipientPlayer() : request.getInitiatorPlayer();

            List<Integer> hours = FormatUtils.parseHoursFromJson(request.getProposedHours());
            String timeStr = "";
            if (!hours.isEmpty()) {
                timeStr = FormatUtils.formatHours(hours);
            }

            String notification = localizationService.msg(opponent, "match.request.book.notification",
                responsiblePlayer.getName(),
                FormatUtils.formatDate(request.getProposedDate()),
                timeStr);
            messageSender.accept(opponent.getTelegramId(), notification);
        }
    }

    @Transactional
    public void unbookRequestLocalized(Long requestId, Long unbookingPlayerId, LocalizationService localizationService, BiConsumer<Long, String> messageSender) {
        ScheduleRequest request = scheduleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getStatus() != ScheduleRequest.ScheduleStatus.Booked) {
            throw new IllegalArgumentException(localizationService.msg(null, "match.request.unbook.only.booked"));
        }

        Tour tour = request.getTour();
        Player responsiblePlayer = tour.getResponsiblePlayer();

        if (responsiblePlayer == null || !responsiblePlayer.getId().equals(unbookingPlayerId)) {
            throw new IllegalArgumentException(localizationService.msg(null, "match.request.unbook.only.responsible"));
        }

        request.setStatus(ScheduleRequest.ScheduleStatus.Accepted);
        request.setUpdatedAt(LocalDateTime.now());
        scheduleRequestRepository.save(request);

        if (messageSender != null) {
            Player opponent = request.getInitiatorPlayer().getId().equals(unbookingPlayerId) ?
                request.getRecipientPlayer() : request.getInitiatorPlayer();

            String notification = localizationService.msg(opponent, "match.request.unbook.notification",
                responsiblePlayer.getName(),
                FormatUtils.formatDate(request.getProposedDate()));
            messageSender.accept(opponent.getTelegramId(), notification);
        }
    }
}

