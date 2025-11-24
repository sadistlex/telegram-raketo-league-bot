package com.raketo.league.telegram.handler;

import com.raketo.league.model.*;
import com.raketo.league.service.*;
import com.raketo.league.telegram.BotCommand;
import com.raketo.league.telegram.TelegramBot;
import com.raketo.league.util.FormatUtils;
import com.raketo.league.util.PlayerContextHolder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class PlayerCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(PlayerCommandHandler.class);
    private final PlayerService playerService;
    private final ScheduleService scheduleService;
    private final AdminService adminService;
    private final AvailabilityService availabilityService;
    private final ScheduleRequestService scheduleRequestService;
    private final LocalizationService localizationService;
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    @Value("${app.webapp.enabled:false}")
    private boolean webappEnabled;

    public void handleCommand(Update update, TelegramBot bot) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        String username = update.getMessage().getFrom().getUserName();
        try {
            Player maybePlayer = playerService.findByTelegramId(userId).orElse(null);
            if (maybePlayer != null) {
                PlayerContextHolder.setCurrentPlayerId(maybePlayer.getId());
            }
            if (BotCommand.START.matches(text)) {
                handleStartCommand(chatId, userId, bot);
            } else if (BotCommand.SCHEDULE.matches(text)) {
                handleSchedule(chatId, userId, username, bot);
            } else if (BotCommand.HELP.matches(text)) {
                handleHelp(chatId, userId, bot);
            } else {
                Player p = playerService.findByTelegramId(userId).orElse(null);
                bot.sendMessage(chatId, localizationService.msg(p, "player.unknown.command", BotCommand.HELP.getCommand()));
            }
        } finally {
            PlayerContextHolder.clear();
        }
    }

    public void handleCallback(Update update, TelegramBot bot) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Long userId = update.getCallbackQuery().getFrom().getId();
        String username = update.getCallbackQuery().getFrom().getUserName();
        Player player = playerService.findOrLinkPlayer(userId, username);
        try {
            if (player != null) {
                PlayerContextHolder.setCurrentPlayerId(player.getId());
            }

            if ("PLAYER_SCHEDULE_REFRESH".equals(callbackData) || "PLAYER_SCHEDULE".equals(callbackData)) {
                handleSchedule(chatId, userId, username, bot);
            } else if (callbackData.startsWith("SCHEDULE_DIVISION_")) {
                Long divisionTournamentId = Long.parseLong(callbackData.substring("SCHEDULE_DIVISION_".length()));
                handleScheduleForDivision(chatId, userId, username, divisionTournamentId, bot);
            } else if ("PLAYER_HELP".equals(callbackData)) {
                handleHelp(chatId, userId, bot);
            } else if ("PLAYER_CONFIG".equals(callbackData)) {
                sendLanguageConfig(chatId, player, bot);
            } else if ("PLAYER_COURTS".equals(callbackData)) {
                handleCourtsSetup(chatId, player, bot);
            } else if (callbackData.startsWith("COURT_SELECT_")) {
                if (player == null) {
                    bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
                    return;
                }
                String court = callbackData.substring("COURT_SELECT_".length());
                handleCourtSelection(chatId, player, court, bot);
            } else if ("COURT_RESET".equals(callbackData)) {
                if (player == null) {
                    bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
                    return;
                }
                playerService.updatePreferredCourts(player, "");
                Player reloaded = playerService.findById(player.getId()).orElse(player);
                showCourtsSelectionScreen(chatId, reloaded, bot);
            } else if ("COURT_FINISH".equals(callbackData)) {
                handleCourtFinish(chatId, player, bot);
            } else if ("PLAYER_MENU".equals(callbackData)) {
                handleStartCommand(chatId, userId, bot);
            } else if ("LANG_RU".equals(callbackData)) {
                if (player != null) {
                    playerService.updateLanguage(player, Language.RU);
                }
                bot.sendMessage(chatId, localizationService.msg(player, "config.language.updated", "Russian"));
                handleStartCommand(chatId, userId, bot);
            } else if ("LANG_EN".equals(callbackData)) {
                if (player != null) {
                    playerService.updateLanguage(player, Language.EN);
                }
                bot.sendMessage(chatId, localizationService.msg(player, "config.language.updated", "English"));
                handleStartCommand(chatId, userId, bot);
            } else if (callbackData.startsWith("MANAGE_TOUR_")) {
                Long tourId = Long.parseLong(callbackData.substring("MANAGE_TOUR_".length()));
                handleManageTour(chatId, userId, username, tourId, bot);
            } else if (callbackData.startsWith("VIEW_REQUESTS_ACCEPTED_")) {
                Long tourId = Long.parseLong(callbackData.substring("VIEW_REQUESTS_ACCEPTED_".length()));
                handleViewRequests(chatId, userId, username, tourId, bot, true);
            } else if (callbackData.startsWith("VIEW_REQUESTS_ALL_")) {
                Long tourId = Long.parseLong(callbackData.substring("VIEW_REQUESTS_ALL_".length()));
                handleViewRequests(chatId, userId, username, tourId, bot, false);
            } else if (callbackData.startsWith("VIEW_REQUESTS_")) {
                Long tourId = Long.parseLong(callbackData.substring("VIEW_REQUESTS_".length()));
                handleViewRequests(chatId, userId, username, tourId, bot, false);
            } else if (callbackData.startsWith("ACCEPT_REQUEST_")) {
                Long requestId = Long.parseLong(callbackData.substring("ACCEPT_REQUEST_".length()));
                handleAcceptRequest(chatId, userId, username, requestId, bot);
            } else if (callbackData.startsWith("DECLINE_REQUEST_")) {
                Long requestId = Long.parseLong(callbackData.substring("DECLINE_REQUEST_".length()));
                handleDeclineRequest(chatId, userId, username, requestId, bot);
            } else if (callbackData.startsWith("CANCEL_REQUEST_")) {
                Long requestId = Long.parseLong(callbackData.substring("CANCEL_REQUEST_".length()));
                handleCancelRequest(chatId, userId, username, requestId, bot);
            } else if (callbackData.startsWith("CHANGE_TO_ACCEPT_")) {
                Long requestId = Long.parseLong(callbackData.substring("CHANGE_TO_ACCEPT_".length()));
                handleChangeRequestStatus(chatId, userId, username, requestId, ScheduleRequest.ScheduleStatus.Accepted, bot);
            } else if (callbackData.startsWith("CHANGE_TO_DECLINE_")) {
                Long requestId = Long.parseLong(callbackData.substring("CHANGE_TO_DECLINE_".length()));
                handleChangeRequestStatus(chatId, userId, username, requestId, ScheduleRequest.ScheduleStatus.Declined, bot);
            } else if (callbackData.startsWith("BOOK_REQUEST_")) {
                Long requestId = Long.parseLong(callbackData.substring("BOOK_REQUEST_".length()));
                handleBookRequest(chatId, userId, username, requestId, bot);
            } else if (callbackData.startsWith("UNBOOK_REQUEST_")) {
                Long requestId = Long.parseLong(callbackData.substring("UNBOOK_REQUEST_".length()));
                handleUnbookRequest(chatId, userId, username, requestId, bot);
            } else if (callbackData.startsWith("COMPLETE_TOUR_")) {
                Long tourId = Long.parseLong(callbackData.substring("COMPLETE_TOUR_".length()));
                handleCompleteTour(chatId, userId, username, tourId, bot);
            } else if (callbackData.startsWith("POSTPONE_TOUR_")) {
                Long tourId = Long.parseLong(callbackData.substring("POSTPONE_TOUR_".length()));
                handlePostponeTour(chatId, userId, username, tourId, bot);
            } else if (callbackData.startsWith("CONFIRM_COMPLETE_")) {
                Long tourId = Long.parseLong(callbackData.substring("CONFIRM_COMPLETE_".length()));
                confirmCompleteTour(chatId, userId, username, tourId, bot);
            } else if (callbackData.startsWith("CONFIRM_POSTPONE_")) {
                Long tourId = Long.parseLong(callbackData.substring("CONFIRM_POSTPONE_".length()));
                confirmPostponeTour(chatId, userId, username, tourId, bot);
            }
        } finally {
            PlayerContextHolder.clear();
        }
    }

    private void handleStartCommand(Long chatId, Long userId, TelegramBot bot) {
        boolean isAdmin = adminService.isAdmin(userId);
        Player player = playerService.findByTelegramId(userId).orElse(null);
        String message = localizationService.msg(player, "player.menu.welcome");
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .replyMarkup(createPlayerMenuKeyboard(isAdmin, player))
                .build();
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            logger.error("Failed to send start menu", e);
        }
    }

    private InlineKeyboardMarkup createPlayerMenuKeyboard(boolean isAdmin, Player player) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton scheduleBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "player.menu.schedule_button"))
                .callbackData("PLAYER_SCHEDULE")
                .build();
        keyboard.add(List.of(scheduleBtn));

        InlineKeyboardButton courtsBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "player.menu.courts_button"))
                .callbackData("PLAYER_COURTS")
                .build();
        keyboard.add(List.of(courtsBtn));

        InlineKeyboardButton helpBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "player.menu.help_button"))
                .callbackData("PLAYER_HELP")
                .build();
        keyboard.add(List.of(helpBtn));
        InlineKeyboardButton configBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "player.menu.config_button"))
                .callbackData("PLAYER_CONFIG")
                .build();
        keyboard.add(List.of(configBtn));
        if (isAdmin) {
            InlineKeyboardButton adminBtn = InlineKeyboardButton.builder()
                    .text(localizationService.msg(player, "player.menu.admin_button"))
                    .callbackData("ADMIN_MENU")
                    .build();
            keyboard.add(List.of(adminBtn));
        }
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private void handleSchedule(Long chatId, Long userId, String username, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }

        List<PlayerDivisionAssignment> divisions = scheduleService.getPlayerDivisions(player);

        if (divisions.size() > 1) {
            showDivisionSelection(chatId, player, divisions, bot);
        } else {
            Long divisionTournamentId = divisions.isEmpty() ? null : divisions.getFirst().getDivisionTournament().getId();
            ScheduleService.PlayerSchedule ps = scheduleService.buildPlayerSchedule(player);
            String messageText = renderScheduleMessageLocalized(ps, player);
            SendMessage message = SendMessage.builder().chatId(chatId.toString()).text(messageText).replyMarkup(scheduleKeyboardWithTours(ps, player, divisionTournamentId)).build();
            try {
                bot.execute(message);
            } catch (Exception e) {
                logger.error("Failed to send schedule", e);
            }
        }
    }

    private void showDivisionSelection(Long chatId, Player player, List<PlayerDivisionAssignment> divisions, TelegramBot bot) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (PlayerDivisionAssignment assignment : divisions) {
            DivisionTournament dt = assignment.getDivisionTournament();
            String buttonText = localizationService.msg(player, "player.schedule.division_button",
                dt.getDivision().getName(),
                dt.getTournament().getName());

            InlineKeyboardButton divisionBtn = InlineKeyboardButton.builder()
                    .text(buttonText)
                    .callbackData("SCHEDULE_DIVISION_" + dt.getId())
                    .build();
            keyboard.add(List.of(divisionBtn));
        }

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.msg(player, "player.schedule.select_division"))
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(keyboard).build())
                .build();

        try {
            bot.execute(message);
        } catch (Exception e) {
            logger.error("Failed to send division selection", e);
        }
    }

    private void handleScheduleForDivision(Long chatId, Long userId, String username, Long divisionTournamentId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }

        ScheduleService.PlayerSchedule ps = scheduleService.buildPlayerScheduleForDivision(player, divisionTournamentId);
        String messageText = renderScheduleMessageLocalized(ps, player);
        SendMessage message = SendMessage.builder().chatId(chatId.toString()).text(messageText).replyMarkup(scheduleKeyboardWithTours(ps, player, divisionTournamentId)).build();
        try {
            bot.execute(message);
        } catch (Exception e) {
            logger.error("Failed to send schedule for division", e);
        }
    }

    private String renderScheduleMessageLocalized(ScheduleService.PlayerSchedule ps, Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(localizationService.msg(player, "schedule.header", ps.player().getName(), ps.player().getTelegramUsername()));
        if (ps.tours().isEmpty()) {
            sb.append(localizationService.msg(player, "schedule.none"));
        } else {
            int tourNumber = 1;
            for (ScheduleService.TourInfo ti : ps.tours()) {
                String start = ScheduleService.DATE_FMT.format(ti.startDate());
                String end = ScheduleService.DATE_FMT.format(ti.endDate());
                String opponent;
                if (ti.opponent() != null) {
                    opponent = ti.opponent().getName();
                } else {
                    opponent = localizationService.msg(player, "schedule.bye");
                }
                sb.append(localizationService.msg(player, "schedule.tour.number", tourNumber)).append("\n");
                if (ti.status() != null) {
                    String statusKey = "tour.statuses." + ti.status().name();
                    String localizedStatus = localizationService.msg(player, statusKey);
                    sb.append(localizationService.msg(player, "schedule.tour.status", localizedStatus)).append("\n");
                }
                sb.append(localizationService.msg(player, "schedule.tour.dates", start, end)).append("\n");
                sb.append(localizationService.msg(player, "schedule.tour.opponent_name", opponent)).append("\n");

                if (ti.opponent() != null && ti.responsiblePlayer() != null) {
                    if (ti.responsiblePlayer().getId().equals(ps.player().getId())) {
                        sb.append(localizationService.msg(player, "schedule.tour.responsible.you")).append("\n");
                    } else {
                        sb.append(localizationService.msg(player, "schedule.tour.responsible.opponent", ti.responsiblePlayer().getName())).append("\n");
                    }
                }

                if (ti.opponent() != null) {
                    List<AvailabilitySlot> playerSlots = availabilityService.getPlayerTourAvailability(ps.player().getId(), ti.tourId()).map(List::of).orElse(List.of());
                    List<AvailabilitySlot> opponentSlots = availabilityService.getPlayerTourAvailability(ti.opponent().getId(), ti.tourId()).map(List::of).orElse(List.of());
                    String playerStatus;
                    if (playerSlots.isEmpty()) {
                        playerStatus = localizationService.msg(player, "schedule.status.not.set");
                    } else {
                        playerStatus = localizationService.msg(player, "schedule.status.set");
                    }
                    String opponentStatus;
                    if (opponentSlots.isEmpty()) {
                        opponentStatus = localizationService.msg(player, "schedule.status.not.set");
                    } else {
                        opponentStatus = localizationService.msg(player, "schedule.status.set");
                    }
                    if (ti.status() == Tour.TourStatus.Scheduled && ti.scheduledTime() != null) {
                        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(java.time.ZoneId.of("Asia/Tbilisi"));
                        sb.append(localizationService.msg(player, "schedule.tour.scheduled", timeFmt.format(ti.scheduledTime()))).append("\n");
                    } else {
                        sb.append(localizationService.msg(player, "schedule.tour.availability")).append("\n");
                        sb.append(localizationService.msg(player, "schedule.tour.you", playerStatus)).append("\n");
                        sb.append(localizationService.msg(player, "schedule.tour.opponent.status", opponentStatus)).append("\n");
                    }
                }
                sb.append("\n");
                tourNumber++;
            }
        }
        return sb.toString();
    }

    private InlineKeyboardMarkup scheduleKeyboardWithTours(ScheduleService.PlayerSchedule schedule, Player player, Long divisionTournamentId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        if (webappEnabled) {
            int tourNumber = 1;
            for (ScheduleService.TourInfo ti : schedule.tours()) {
                if (ti.tourId() != null && ti.opponent() != null) {
                    List<ScheduleRequest> tourRequests = scheduleRequestService.getTourRequests(ti.tourId(), schedule.player().getId());
                    long incomingPending = tourRequests.stream()
                            .filter(r -> r.getRecipientPlayer().getId().equals(schedule.player().getId()) && r.getStatus() == ScheduleRequest.ScheduleStatus.Pending)
                            .count();
                    long outgoingPending = tourRequests.stream()
                            .filter(r -> r.getInitiatorPlayer().getId().equals(schedule.player().getId()) && r.getStatus() == ScheduleRequest.ScheduleStatus.Pending)
                            .count();

                    String buttonText = localizationService.msg(player, "player.schedule.manage_tour", tourNumber);
                    if (incomingPending > 0 || outgoingPending > 0) {
                        buttonText += " " + localizationService.msg(player, "player.schedule.requests_count", incomingPending, outgoingPending);
                    }

                    InlineKeyboardButton manageTourBtn = InlineKeyboardButton.builder()
                            .text(buttonText)
                            .callbackData("MANAGE_TOUR_" + ti.tourId())
                            .build();
                    keyboard.add(List.of(manageTourBtn));
                }
                tourNumber++;
            }
        }
        InlineKeyboardButton mainMenuBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "player.menu.back"))
                .callbackData("PLAYER_MENU")
                .build();
        keyboard.add(List.of(mainMenuBtn));

        String refreshCallbackData = divisionTournamentId != null ?
            "SCHEDULE_DIVISION_" + divisionTournamentId : "PLAYER_SCHEDULE_REFRESH";
        InlineKeyboardButton refresh = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "player.schedule.refresh"))
                .callbackData(refreshCallbackData)
                .build();
        keyboard.add(List.of(refresh));
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private void handleHelp(Long chatId, Long userId, TelegramBot bot) {
        boolean isAdmin = adminService.isAdmin(userId);
        Player player = playerService.findByTelegramId(userId).orElse(null);
        StringBuilder helpMessage = new StringBuilder();
        helpMessage.append(localizationService.msg(player, "player.help.header"));
        helpMessage.append(localizationService.msg(player, "player.help.schedule", BotCommand.SCHEDULE.getCommand()));
        helpMessage.append(localizationService.msg(player, "player.help.help", BotCommand.HELP.getCommand()));
        helpMessage.append(localizationService.msg(player, "player.help.features"));
        if (isAdmin) {
            helpMessage.append(localizationService.msg(player, "player.help.admin.header"));
            helpMessage.append(BotCommand.ADMIN.getCommand()).append(" - /admin\n");
        }
        bot.sendMessage(chatId, helpMessage.toString());
    }

    private void handleViewRequests(Long chatId, Long userId, String username, Long tourId, TelegramBot bot, boolean acceptedOnly) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }
        List<ScheduleRequest> requests = scheduleRequestService.getTourRequests(tourId, player.getId());

        if (acceptedOnly) {
            requests = requests.stream()
                    .filter(r -> r.getStatus() == ScheduleRequest.ScheduleStatus.Accepted)
                    .toList();
        }

        String messageText = scheduleRequestService.formatRequestsMessageLocalized(requests, player, localizationService);
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (ScheduleRequest req : requests) {
            List<Integer> hours = FormatUtils.parseHoursFromJson(req.getProposedHours());
            String timeLabelShort = FormatUtils.formatDateNoYear(req.getProposedDate());
            if (!hours.isEmpty()) {
                timeLabelShort += " " + FormatUtils.formatHours(hours);
            }

            // Check if current player is responsible for booking
            Tour tour = req.getTour();
            Player responsiblePlayer = tour.getResponsiblePlayer();
            boolean isResponsible = responsiblePlayer != null && responsiblePlayer.getId().equals(player.getId());

            if (req.getRecipientPlayer().getId().equals(player.getId())) {
                if (req.getStatus() == ScheduleRequest.ScheduleStatus.Pending) {
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    InlineKeyboardButton acceptBtn = InlineKeyboardButton.builder()
                            .text(localizationService.msg(player, "player.requests.accept_button_short", timeLabelShort))
                            .callbackData("ACCEPT_REQUEST_" + req.getId())
                            .build();
                    row.add(acceptBtn);
                    InlineKeyboardButton declineBtn = InlineKeyboardButton.builder()
                            .text(localizationService.msg(player, "player.requests.decline_button_short", timeLabelShort))
                            .callbackData("DECLINE_REQUEST_" + req.getId())
                            .build();
                    row.add(declineBtn);
                    keyboard.add(row);
                } else if (req.getStatus() == ScheduleRequest.ScheduleStatus.Accepted) {
                    InlineKeyboardButton changeToDeclineBtn = InlineKeyboardButton.builder()
                            .text(localizationService.msg(player, "player.requests.change_to_decline_button_short", timeLabelShort))
                            .callbackData("CHANGE_TO_DECLINE_" + req.getId())
                            .build();
                    keyboard.add(List.of(changeToDeclineBtn));
                } else if (req.getStatus() == ScheduleRequest.ScheduleStatus.Declined) {
                    InlineKeyboardButton changeToAcceptBtn = InlineKeyboardButton.builder()
                            .text(localizationService.msg(player, "player.requests.change_to_accept_button_short", timeLabelShort))
                            .callbackData("CHANGE_TO_ACCEPT_" + req.getId())
                            .build();
                    keyboard.add(List.of(changeToAcceptBtn));
                }
            } else if (req.getInitiatorPlayer().getId().equals(player.getId())) {
                if (req.getStatus() != ScheduleRequest.ScheduleStatus.Cancelled) {
                    InlineKeyboardButton cancelBtn = InlineKeyboardButton.builder()
                            .text(localizationService.msg(player, "player.requests.cancel_button_short", timeLabelShort))
                            .callbackData("CANCEL_REQUEST_" + req.getId())
                            .build();
                    keyboard.add(List.of(cancelBtn));
                }
            }

            // Add book/unbook button for responsible player
            if (isResponsible && req.getStatus() == ScheduleRequest.ScheduleStatus.Accepted) {
                InlineKeyboardButton bookBtn = InlineKeyboardButton.builder()
                        .text(localizationService.msg(player, "player.requests.book_button_short", timeLabelShort))
                        .callbackData("BOOK_REQUEST_" + req.getId())
                        .build();
                keyboard.add(List.of(bookBtn));
            } else if (isResponsible && req.getStatus() == ScheduleRequest.ScheduleStatus.Booked) {
                InlineKeyboardButton unbookBtn = InlineKeyboardButton.builder()
                        .text(localizationService.msg(player, "player.requests.unbook_button_short", timeLabelShort))
                        .callbackData("UNBOOK_REQUEST_" + req.getId())
                        .build();
                keyboard.add(List.of(unbookBtn));
            }
        }

        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "player.requests.back"))
                .callbackData("PLAYER_SCHEDULE")
                .build();
        keyboard.add(List.of(backBtn));
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(messageText)
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(keyboard).build())
                .build();
        try {
            bot.execute(message);
        } catch (Exception e) {
            logger.error("Failed to send requests", e);
        }
    }

    private void handleAcceptRequest(Long chatId, Long userId, String username, Long requestId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }
        try {
            scheduleRequestService.acceptRequestLocalized(requestId, player.getId(), localizationService, bot::sendMessage);
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.accepted"));
        } catch (Exception e) {
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.accept.failed", e.getMessage()));
        }
    }

    private void handleDeclineRequest(Long chatId, Long userId, String username, Long requestId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }
        try {
            scheduleRequestService.declineRequestLocalized(requestId, player.getId(), localizationService, bot::sendMessage);
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.declined"));
        } catch (Exception e) {
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.decline.failed", e.getMessage()));
        }
    }

    private void handleCancelRequest(Long chatId, Long userId, String username, Long requestId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }
        try {
            scheduleRequestService.cancelRequestLocalized(requestId, player.getId(), localizationService, bot::sendMessage);
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.cancelled"));
        } catch (Exception e) {
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.cancel.failed", e.getMessage()));
        }
    }

    private void handleChangeRequestStatus(Long chatId, Long userId, String username, Long requestId,
                                          ScheduleRequest.ScheduleStatus newStatus, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }
        try {
            scheduleRequestService.changeRequestStatusLocalized(requestId, player.getId(), newStatus, localizationService, bot::sendMessage);
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.status_changed"));
        } catch (Exception e) {
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.change.failed", e.getMessage()));
        }
    }

    private void handleBookRequest(Long chatId, Long userId, String username, Long requestId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }
        try {
            scheduleRequestService.bookRequestLocalized(requestId, player.getId(), localizationService, bot::sendMessage);
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.booked"));
        } catch (Exception e) {
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.book.failed", e.getMessage()));
        }
    }

    private void handleUnbookRequest(Long chatId, Long userId, String username, Long requestId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }
        try {
            scheduleRequestService.unbookRequestLocalized(requestId, player.getId(), localizationService, bot::sendMessage);
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.unbooked"));
        } catch (Exception e) {
            bot.sendMessage(chatId, localizationService.msg(player, "player.requests.unbook.failed", e.getMessage()));
        }
    }

    private void handleManageTour(Long chatId, Long userId, String username, Long tourId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }

        // First get the tour to find its division
        ScheduleService.PlayerSchedule fullSchedule = scheduleService.buildPlayerSchedule(player);
        ScheduleService.TourInfo tourInfo = fullSchedule.tours().stream()
                .filter(ti -> ti.tourId() != null && ti.tourId().equals(tourId))
                .findFirst()
                .orElse(null);

        if (tourInfo == null) {
            bot.sendMessage(chatId, localizationService.msg(player, "tour.not.found"));
            return;
        }

        // Get division-specific schedule to calculate correct tour number
        Long divisionTournamentId = tourInfo.divisionTournamentId();
        ScheduleService.PlayerSchedule divisionSchedule = scheduleService.buildPlayerScheduleForDivision(player, divisionTournamentId);
        int tourNumber = divisionSchedule.tours().indexOf(
            divisionSchedule.tours().stream()
                .filter(ti -> ti.tourId() != null && ti.tourId().equals(tourId))
                .findFirst()
                .orElse(null)
        ) + 1;
        StringBuilder message = new StringBuilder();
        message.append(localizationService.msg(player, "tour.manage.header", tourNumber)).append("\n\n");

        String start = ScheduleService.DATE_FMT.format(tourInfo.startDate());
        String end = ScheduleService.DATE_FMT.format(tourInfo.endDate());
        message.append(localizationService.msg(player, "tour.manage.dates", start, end)).append("\n");

        if (tourInfo.opponent() != null) {
            String opponentInfo = tourInfo.opponent().getName();
            if (tourInfo.opponent().getTelegramUsername() != null && !tourInfo.opponent().getTelegramUsername().isEmpty()) {
                opponentInfo += " (@" + tourInfo.opponent().getTelegramUsername() + ")";
            }
            message.append(localizationService.msg(player, "tour.manage.tour_opponent", opponentInfo)).append("\n");

            if (tourInfo.responsiblePlayer() != null) {
                if (tourInfo.responsiblePlayer().getId().equals(player.getId())) {
                    message.append(localizationService.msg(player, "tour.manage.responsible.you")).append("\n");
                } else {
                    message.append(localizationService.msg(player, "tour.manage.responsible.opponent", tourInfo.responsiblePlayer().getName())).append("\n");
                }
            }

            String opponentCourts = tourInfo.opponent().getPreferredCourts();
            if (opponentCourts != null && !opponentCourts.isEmpty()) {
                String[] courts = opponentCourts.split(",");
                StringBuilder courtsList = new StringBuilder();
                for (int i = 0; i < courts.length; i++) {
                    if (i > 0) courtsList.append(", ");
                    courtsList.append(getCourtDisplayName(courts[i], player));
                }
                message.append(localizationService.msg(player, "player.courts.setup.opponent", courtsList.toString())).append("\n");
            } else {
                message.append(localizationService.msg(player, "player.courts.opponent.not_set")).append("\n");
            }
        }

        List<AvailabilitySlot> playerSlots = availabilityService.getPlayerTourAvailability(player.getId(), tourId).map(List::of).orElse(List.of());
        List<AvailabilitySlot> opponentSlots = tourInfo.opponent() != null ?
                availabilityService.getPlayerTourAvailability(tourInfo.opponent().getId(), tourId).map(List::of).orElse(List.of()) :
                List.of();

        String playerStatus = playerSlots.isEmpty() ?
                localizationService.msg(player, "schedule.status.not.set") :
                localizationService.msg(player, "schedule.status.set");
        message.append(localizationService.msg(player, "tour.manage.availability.you", playerStatus)).append("\n");

        if (tourInfo.opponent() != null) {
            String opponentStatus = opponentSlots.isEmpty() ?
                    localizationService.msg(player, "schedule.status.not.set") :
                    localizationService.msg(player, "schedule.status.set");
            message.append(localizationService.msg(player, "tour.manage.availability.opponent", opponentStatus)).append("\n");
        }

        List<ScheduleRequest> tourRequests = scheduleRequestService.getTourRequests(tourId, player.getId());
        long incomingPending = tourRequests.stream()
                .filter(r -> r.getRecipientPlayer().getId().equals(player.getId()) && r.getStatus() == ScheduleRequest.ScheduleStatus.Pending)
                .count();
        long outgoingPending = tourRequests.stream()
                .filter(r -> r.getInitiatorPlayer().getId().equals(player.getId()) && r.getStatus() == ScheduleRequest.ScheduleStatus.Pending)
                .count();

        if (incomingPending > 0 || outgoingPending > 0) {
            message.append("\n").append(localizationService.msg(player, "tour.manage.requests.summary", incomingPending, outgoingPending)).append("\n");
        }

        if (tourInfo.status() == Tour.TourStatus.Scheduled && tourInfo.scheduledTime() != null) {
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(java.time.ZoneId.of("Asia/Tbilisi"));
            message.append("\n").append(localizationService.msg(player, "tour.manage.scheduled", timeFmt.format(tourInfo.scheduledTime()))).append("\n");
        }

        if (tourInfo.status() != null) {
            String statusKey = "tour.statuses." + tourInfo.status().name();
            String localizedStatus = localizationService.msg(player, statusKey);
            message.append("\n").append(localizationService.msg(player, "tour.manage.status", localizedStatus));
        } else {
            message.append("\n").append(localizationService.msg(player, "tour.manage.status", ""));
        }

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        String availabilityButtonKey = playerSlots.isEmpty() ?
                "tour.manage.button.availability" : "tour.manage.button.change_availability";
        InlineKeyboardButton setAvailabilityBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, availabilityButtonKey))
                .webApp(new WebAppInfo(baseUrl + "/webapp/calendar?playerId=" + player.getTelegramId() + "&tourId=" + tourId))
                .build();
        keyboard.add(List.of(setAvailabilityBtn));

        if (!playerSlots.isEmpty() && !opponentSlots.isEmpty()) {
            InlineKeyboardButton compatibleBtn = InlineKeyboardButton.builder()
                    .text(localizationService.msg(player, "tour.manage.button.compatible"))
                    .webApp(new WebAppInfo(baseUrl + "/webapp/compatible?playerId=" + player.getId() + "&opponentId=" + tourInfo.opponent().getId() + "&tourId=" + tourId))
                    .build();
            keyboard.add(List.of(compatibleBtn));
        }

        InlineKeyboardButton acceptedRequestsBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "tour.manage.button.requests.accepted"))
                .callbackData("VIEW_REQUESTS_ACCEPTED_" + tourId)
                .build();
        InlineKeyboardButton allRequestsBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "tour.manage.button.requests.all"))
                .callbackData("VIEW_REQUESTS_ALL_" + tourId)
                .build();
        keyboard.add(List.of(acceptedRequestsBtn, allRequestsBtn));

        if (tourInfo.status() == Tour.TourStatus.Active || tourInfo.status() == Tour.TourStatus.Scheduled || tourInfo.status() == Tour.TourStatus.Postponed) {
            InlineKeyboardButton completeBtn = InlineKeyboardButton.builder()
                    .text(localizationService.msg(player, "tour.manage.button.complete"))
                    .callbackData("COMPLETE_TOUR_" + tourId)
                    .build();
            keyboard.add(List.of(completeBtn));
        }

        if (tourInfo.status() == Tour.TourStatus.Active || tourInfo.status() == Tour.TourStatus.Scheduled) {
            InlineKeyboardButton postponeBtn = InlineKeyboardButton.builder()
                    .text(localizationService.msg(player, "tour.manage.button.postpone"))
                    .callbackData("POSTPONE_TOUR_" + tourId)
                    .build();
            keyboard.add(List.of(postponeBtn));
        }

        String backCallbackData = divisionTournamentId != null ?
            "SCHEDULE_DIVISION_" + divisionTournamentId : "PLAYER_SCHEDULE";
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "tour.manage.button.back"))
                .callbackData(backCallbackData)
                .build();
        keyboard.add(List.of(backBtn));

        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message.toString())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(keyboard).build())
                .build();

        try {
            bot.execute(msg);
        } catch (Exception e) {
            logger.error("Failed to send manage tour screen", e);
        }
    }

    private void handleCompleteTour(Long chatId, Long userId, String username, Long tourId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton confirmBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "tour.complete.confirm"))
                .callbackData("CONFIRM_COMPLETE_" + tourId)
                .build();
        InlineKeyboardButton cancelBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "tour.complete.cancel"))
                .callbackData("MANAGE_TOUR_" + tourId)
                .build();
        keyboard.add(List.of(confirmBtn, cancelBtn));

        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.msg(player, "tour.complete.confirmation"))
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(keyboard).build())
                .build();

        try {
            bot.execute(msg);
        } catch (Exception e) {
            logger.error("Failed to show complete confirmation", e);
        }
    }

    private void handlePostponeTour(Long chatId, Long userId, String username, Long tourId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton confirmBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "tour.postpone.confirm"))
                .callbackData("CONFIRM_POSTPONE_" + tourId)
                .build();
        InlineKeyboardButton cancelBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "tour.postpone.cancel"))
                .callbackData("MANAGE_TOUR_" + tourId)
                .build();
        keyboard.add(List.of(confirmBtn, cancelBtn));

        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.msg(player, "tour.postpone.confirmation"))
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(keyboard).build())
                .build();

        try {
            bot.execute(msg);
        } catch (Exception e) {
            logger.error("Failed to show postpone confirmation", e);
        }
    }

    private void confirmCompleteTour(Long chatId, Long userId, String username, Long tourId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }

        try {
            scheduleRequestService.completeTour(tourId);
            bot.sendMessage(chatId, localizationService.msg(player, "tour.complete.success"));
            handleSchedule(chatId, userId, username, bot);
        } catch (Exception e) {
            bot.sendMessage(chatId, localizationService.msg(player, "tour.complete.failed", e.getMessage()));
        }
    }

    private void confirmPostponeTour(Long chatId, Long userId, String username, Long tourId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }

        try {
            scheduleRequestService.postponeTour(tourId);
            bot.sendMessage(chatId, localizationService.msg(player, "tour.postpone.success"));
            handleSchedule(chatId, userId, username, bot);
        } catch (Exception e) {
            bot.sendMessage(chatId, localizationService.msg(player, "tour.postpone.failed", e.getMessage()));
        }
    }

    private void sendLanguageConfig(Long chatId, Player player, TelegramBot bot) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton ruBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "config.language.ru_button"))
                .callbackData("LANG_RU")
                .build();
        InlineKeyboardButton enBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "config.language.en_button"))
                .callbackData("LANG_EN")
                .build();
        keyboard.add(List.of(ruBtn, enBtn));
        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(localizationService.msg(player, "config.language.header"))
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(keyboard).build())
                .build();
        try {
            bot.execute(msg);
        } catch (Exception e) {
            logger.error("Failed to send language config", e);
        }
    }

    private void handleCourtsSetup(Long chatId, Player player, TelegramBot bot) {
        showCourtsSelectionScreen(chatId, player, bot);
    }

    private void handleCourtSelection(Long chatId, Player player, String court, TelegramBot bot) {
        String currentCourts = player.getPreferredCourts();
        if (currentCourts == null) currentCourts = "";

        String[] parts = currentCourts.isEmpty() ? new String[0] : currentCourts.split(",");
        for (String p : parts) {
            if (p.equals(court)) {
                showCourtsSelectionScreen(chatId, player, bot);
                return;
            }
        }

        String newCourts = currentCourts.isEmpty() ? court : currentCourts + "," + court;

        playerService.updatePreferredCourts(player, newCourts);

        Player reloaded = playerService.findById(player.getId()).orElse(player);
        showCourtsSelectionScreen(chatId, reloaded, bot);
    }

    private void handleCourtFinish(Long chatId, Player player, TelegramBot bot) {
        bot.sendMessage(chatId, localizationService.msg(player, "player.courts.setup.saved"));
        handleStartCommand(chatId, player.getTelegramId(), bot);
    }

    private void showCourtsSelectionScreen(Long chatId, Player player, TelegramBot bot) {
        StringBuilder message = new StringBuilder();
        message.append(localizationService.msg(player, "player.courts.setup.header"));

        String currentSelection = player.getPreferredCourts();
        if (currentSelection == null || currentSelection.isEmpty()) {
            message.append(localizationService.msg(player, "player.courts.setup.none"));
        } else {
            String[] courts = currentSelection.split(",");
            for (int i = 0; i < courts.length; i++) {
                message.append("\n").append(i + 1).append(". ").append(getCourtDisplayName(courts[i], player));
            }
        }

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        Set<String> chosen = new HashSet<>();
        if (currentSelection != null && !currentSelection.isEmpty()) {
            chosen.addAll(Arrays.asList(currentSelection.split(",")));
        }

        for (Court court : Court.values()) {
            if (chosen.contains(court.name())) {
                continue;
            }
            String buttonText = court == Court.OTHER
                ? localizationService.msg(player, "player.courts.setup.other")
                : court.getDisplayName();

            InlineKeyboardButton courtBtn = InlineKeyboardButton.builder()
                    .text(buttonText)
                    .callbackData("COURT_SELECT_" + court.name())
                    .build();
            keyboard.add(List.of(courtBtn));
        }

        InlineKeyboardButton resetBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "player.courts.setup.reset_button"))
                .callbackData("COURT_RESET")
                .build();
        keyboard.add(List.of(resetBtn));

        InlineKeyboardButton finishBtn = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "player.courts.setup.finish_button"))
                .callbackData("COURT_FINISH")
                .build();
        keyboard.add(List.of(finishBtn));

        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message.toString())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(keyboard).build())
                .build();

        try {
            bot.execute(msg);
        } catch (Exception e) {
            logger.error("Failed to send courts selection screen", e);
        }
    }

    private String getCourtDisplayName(String courtCode, Player player) {
        try {
            Court court = Court.valueOf(courtCode);
            return court == Court.OTHER
                ? localizationService.msg(player, "player.courts.setup.other")
                : court.getDisplayName();
        } catch (IllegalArgumentException e) {
            return courtCode;
        }
    }
}
