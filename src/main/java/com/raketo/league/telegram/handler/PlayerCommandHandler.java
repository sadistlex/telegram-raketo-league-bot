package com.raketo.league.telegram.handler;

import com.raketo.league.model.*;
import com.raketo.league.service.*;
import com.raketo.league.telegram.BotCommand;
import com.raketo.league.telegram.TelegramBot;
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
import java.util.ArrayList;
import java.util.List;

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
    }

    public void handleCallback(Update update, TelegramBot bot) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Long userId = update.getCallbackQuery().getFrom().getId();
        String username = update.getCallbackQuery().getFrom().getUserName();
        Player player = playerService.findOrLinkPlayer(userId, username);

        if ("PLAYER_SCHEDULE_REFRESH".equals(callbackData) || "PLAYER_SCHEDULE".equals(callbackData)) {
            handleSchedule(chatId, userId, username, bot);
        } else if ("PLAYER_HELP".equals(callbackData)) {
            handleHelp(chatId, userId, bot);
        } else if ("PLAYER_CONFIG".equals(callbackData)) {
            sendLanguageConfig(chatId, player, bot);
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
        } else if (callbackData.startsWith("VIEW_REQUESTS_")) {
            Long tourId = Long.parseLong(callbackData.substring("VIEW_REQUESTS_".length()));
            handleViewRequests(chatId, userId, username, tourId, bot);
        } else if (callbackData.startsWith("ACCEPT_REQUEST_")) {
            Long requestId = Long.parseLong(callbackData.substring("ACCEPT_REQUEST_".length()));
            handleAcceptRequest(chatId, userId, username, requestId, bot);
        } else if (callbackData.startsWith("DECLINE_REQUEST_")) {
            Long requestId = Long.parseLong(callbackData.substring("DECLINE_REQUEST_".length()));
            handleDeclineRequest(chatId, userId, username, requestId, bot);
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
        ScheduleService.PlayerSchedule ps = scheduleService.buildPlayerSchedule(player);
        String messageText = renderScheduleMessageLocalized(ps, player);
        SendMessage message = SendMessage.builder().chatId(chatId.toString()).text(messageText).replyMarkup(scheduleKeyboardWithTours(ps, player)).build();
        try {
            bot.execute(message);
        } catch (Exception e) {
            logger.error("Failed to send schedule", e);
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
                sb.append(localizationService.msg(player, "schedule.tour.line", tourNumber, start, end, opponent));
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
                        sb.append(" ").append(timeFmt.format(ti.scheduledTime()));
                    } else {
                        sb.append(localizationService.msg(player, "schedule.availability.status", playerStatus, opponentStatus));
                    }
                }
                sb.append("\n");
                tourNumber++;
            }
        }
        return sb.toString();
    }

    private InlineKeyboardMarkup scheduleKeyboardWithTours(ScheduleService.PlayerSchedule schedule, Player player) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        if (webappEnabled) {
            int tourNumber = 1;
            for (ScheduleService.TourInfo ti : schedule.tours()) {
                if (ti.tourId() != null && ti.opponent() != null) {
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    InlineKeyboardButton availabilityBtn = InlineKeyboardButton.builder()
                            .text(localizationService.msg(player, "player.schedule.set_availability", tourNumber))
                            .webApp(new WebAppInfo(baseUrl + "/webapp/calendar?playerId=" + schedule.player().getTelegramId() + "&tourId=" + ti.tourId()))
                            .build();
                    row.add(availabilityBtn);
                    keyboard.add(row);
                    boolean playerHasAvailability = availabilityService.getPlayerTourAvailability(schedule.player().getId(), ti.tourId()).isPresent();
                    boolean opponentHasAvailability = availabilityService.getPlayerTourAvailability(ti.opponent().getId(), ti.tourId()).isPresent();
                    List<InlineKeyboardButton> actionsRow = new ArrayList<>();
                    if (playerHasAvailability && opponentHasAvailability) {
                        InlineKeyboardButton compatibleTimesBtn = InlineKeyboardButton.builder()
                                .text(localizationService.msg(player, "player.schedule.compatible_times", tourNumber))
                                .webApp(new WebAppInfo(baseUrl + "/webapp/compatible?playerId=" + schedule.player().getId() + "&opponentId=" + ti.opponent().getId() + "&tourId=" + ti.tourId()))
                                .build();
                        actionsRow.add(compatibleTimesBtn);
                    }
                    InlineKeyboardButton requestsBtn = InlineKeyboardButton.builder()
                            .text(localizationService.msg(player, "player.schedule.view_requests", tourNumber))
                            .callbackData("VIEW_REQUESTS_" + ti.tourId())
                            .build();
                    actionsRow.add(requestsBtn);
                    if (!actionsRow.isEmpty()) {
                        keyboard.add(actionsRow);
                    }
                }
                tourNumber++;
            }
        }
        InlineKeyboardButton refresh = InlineKeyboardButton.builder()
                .text(localizationService.msg(player, "player.schedule.refresh"))
                .callbackData("PLAYER_SCHEDULE_REFRESH")
                .build();
        keyboard.add(List.of(refresh));
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private void handleHelp(Long chatId, Long userId, TelegramBot bot) {
        boolean isAdmin = adminService.isAdmin(userId);
        Player player = playerService.findByTelegramId(userId).orElse(null);
        StringBuilder helpMessage = new StringBuilder();
        helpMessage.append(localizationService.msg(player, "player.help.header"));
        helpMessage.append(localizationService.msg(player, "player.help.schedule", BotCommand.SCHEDULE.getCommand())).append("\n");
        helpMessage.append(localizationService.msg(player, "player.help.help", BotCommand.HELP.getCommand())).append("\n");
        if (isAdmin) {
            helpMessage.append(localizationService.msg(player, "player.help.admin.header"));
            helpMessage.append(BotCommand.ADMIN.getCommand()).append(" - /admin\n");
        }
        bot.sendMessage(chatId, helpMessage.toString());
    }

    private void handleViewRequests(Long chatId, Long userId, String username, Long tourId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, localizationService.resolve(Language.RU, "player.not.registered"));
            return;
        }
        List<ScheduleRequest> requests = scheduleRequestService.getTourRequests(tourId, player.getId());
        String messageText = scheduleRequestService.formatRequestsMessageLocalized(requests, player, localizationService);
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (ScheduleRequest req : requests) {
            if (req.getRecipientPlayer().getId().equals(player.getId()) && req.getStatus() == ScheduleRequest.ScheduleStatus.Pending) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton acceptBtn = InlineKeyboardButton.builder()
                        .text(localizationService.msg(player, "player.requests.accept_button", req.getId()))
                        .callbackData("ACCEPT_REQUEST_" + req.getId())
                        .build();
                row.add(acceptBtn);
                InlineKeyboardButton declineBtn = InlineKeyboardButton.builder()
                        .text(localizationService.msg(player, "player.requests.decline_button", req.getId()))
                        .callbackData("DECLINE_REQUEST_" + req.getId())
                        .build();
                row.add(declineBtn);
                keyboard.add(row);
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
}
