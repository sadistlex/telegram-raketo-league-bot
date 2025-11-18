package com.raketo.league.telegram.handler;

import com.raketo.league.model.Player;
import com.raketo.league.service.AdminService;
import com.raketo.league.service.AvailabilityService;
import com.raketo.league.service.PlayerService;
import com.raketo.league.service.ScheduleRequestService;
import com.raketo.league.service.ScheduleService;
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
            bot.sendMessage(chatId, "Unknown command. Type " + BotCommand.HELP.getCommand() + " for available commands.");
        }
    }

    public void handleCallback(Update update, TelegramBot bot) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Long userId = update.getCallbackQuery().getFrom().getId();
        String username = update.getCallbackQuery().getFrom().getUserName();

        if ("PLAYER_SCHEDULE_REFRESH".equals(callbackData) || "PLAYER_SCHEDULE".equals(callbackData)) {
            handleSchedule(chatId, userId, username, bot);
        } else if ("PLAYER_HELP".equals(callbackData)) {
            handleHelp(chatId, userId, bot);
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

        String message = "Welcome to Raketo League Bot!\n\n" +
                "Use the buttons below to navigate:";

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .replyMarkup(createPlayerMenuKeyboard(isAdmin))
                .build();

        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            logger.error("Failed to send start menu", e);
        }
    }

    private InlineKeyboardMarkup createPlayerMenuKeyboard(boolean isAdmin) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton scheduleBtn = InlineKeyboardButton.builder()
                .text("📅 My Schedule")
                .callbackData("PLAYER_SCHEDULE")
                .build();
        keyboard.add(List.of(scheduleBtn));

        InlineKeyboardButton helpBtn = InlineKeyboardButton.builder()
                .text("❓ Help")
                .callbackData("PLAYER_HELP")
                .build();
        keyboard.add(List.of(helpBtn));

        if (isAdmin) {
            InlineKeyboardButton adminBtn = InlineKeyboardButton.builder()
                    .text("⚙️ Admin Panel")
                    .callbackData("ADMIN_MENU")
                    .build();
            keyboard.add(List.of(adminBtn));
        }

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private void handleSchedule(Long chatId, Long userId, String username, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, "You are not registered. Contact admin.");
            return;
        }
        ScheduleService.PlayerSchedule ps = scheduleService.buildPlayerSchedule(player);
        String messageText = scheduleService.renderScheduleMessage(ps);

        SendMessage message = SendMessage.builder().chatId(chatId.toString()).text(messageText).replyMarkup(scheduleKeyboardWithTours(ps)).build();
        try { bot.execute(message); } catch (Exception e) { logger.error("Failed to send schedule", e); }
    }

    private InlineKeyboardMarkup scheduleKeyboardWithTours(ScheduleService.PlayerSchedule schedule) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        if (webappEnabled) {
            int tourNumber = 1;
            for (ScheduleService.TourInfo ti : schedule.tours()) {
                if (ti.tourId() != null && ti.opponent() != null) {
                    List<InlineKeyboardButton> row = new ArrayList<>();

                    InlineKeyboardButton availabilityBtn = InlineKeyboardButton.builder()
                            .text("Set Availability (Tour " + tourNumber + ")")
                            .webApp(new WebAppInfo(baseUrl + "/webapp/calendar?playerId=" + schedule.player().getTelegramId() + "&tourId=" + ti.tourId()))
                            .build();
                    row.add(availabilityBtn);
                    keyboard.add(row);

                    boolean playerHasAvailability = availabilityService.getPlayerTourAvailability(schedule.player().getId(), ti.tourId()).isPresent();
                    boolean opponentHasAvailability = availabilityService.getPlayerTourAvailability(ti.opponent().getId(), ti.tourId()).isPresent();

                    List<InlineKeyboardButton> actionsRow = new ArrayList<>();

                    if (playerHasAvailability && opponentHasAvailability) {
                        InlineKeyboardButton compatibleTimesBtn = InlineKeyboardButton.builder()
                                .text("Compatible Times (T" + tourNumber + ")")
                                .webApp(new WebAppInfo(baseUrl + "/webapp/compatible?playerId=" + schedule.player().getId() + "&opponentId=" + ti.opponent().getId() + "&tourId=" + ti.tourId()))
                                .build();
                        actionsRow.add(compatibleTimesBtn);
                    }

                    InlineKeyboardButton requestsBtn = InlineKeyboardButton.builder()
                            .text("View Requests (T" + tourNumber + ")")
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

        InlineKeyboardButton refresh = InlineKeyboardButton.builder().text("Refresh").callbackData("PLAYER_SCHEDULE_REFRESH").build();
        keyboard.add(List.of(refresh));
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private void handleHelp(Long chatId, Long userId, TelegramBot bot) {
        boolean isAdmin = adminService.isAdmin(userId);

        StringBuilder helpMessage = new StringBuilder();
        helpMessage.append("Player Commands:\n");
        helpMessage.append(BotCommand.SCHEDULE.getCommand()).append(" - View schedule\n");
        helpMessage.append(BotCommand.HELP.getCommand()).append(" - This help\n");

        if (isAdmin) {
            helpMessage.append("\nAdmin Commands:\n");
            helpMessage.append(BotCommand.ADMIN.getCommand()).append(" - Show admin help\n");
            helpMessage.append(BotCommand.CREATE_TOURNAMENT.getCommand()).append(" - Create a new tournament\n");
            helpMessage.append(BotCommand.ADD_PLAYER.getCommand()).append(" @username - Add a player\n");
            helpMessage.append(BotCommand.GENERATE_TOURS.getCommand()).append(" - Generate tours\n");
        }

        bot.sendMessage(chatId, helpMessage.toString());
    }

    private void handleViewRequests(Long chatId, Long userId, String username, Long tourId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, "You are not registered. Contact admin.");
            return;
        }

        List<com.raketo.league.model.ScheduleRequest> requests = scheduleRequestService.getTourRequests(tourId, player.getId());
        String messageText = scheduleRequestService.formatRequestsMessage(requests, player);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (com.raketo.league.model.ScheduleRequest req : requests) {
            if (req.getRecipientPlayer().getId().equals(player.getId()) &&
                req.getStatus() == com.raketo.league.model.ScheduleRequest.ScheduleStatus.Pending) {

                List<InlineKeyboardButton> row = new ArrayList<>();

                InlineKeyboardButton acceptBtn = InlineKeyboardButton.builder()
                        .text("✅ Accept #" + req.getId())
                        .callbackData("ACCEPT_REQUEST_" + req.getId())
                        .build();
                row.add(acceptBtn);

                InlineKeyboardButton declineBtn = InlineKeyboardButton.builder()
                        .text("❌ Decline #" + req.getId())
                        .callbackData("DECLINE_REQUEST_" + req.getId())
                        .build();
                row.add(declineBtn);

                keyboard.add(row);
            }
        }

        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
                .text("🔙 Back to Schedule")
                .callbackData("PLAYER_SCHEDULE")
                .build();
        keyboard.add(List.of(backBtn));

        org.telegram.telegrambots.meta.api.methods.send.SendMessage message =
                org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
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
            bot.sendMessage(chatId, "You are not registered. Contact admin.");
            return;
        }

        try {
            scheduleRequestService.acceptRequest(requestId, player.getId(), bot::sendMessage);
            bot.sendMessage(chatId, "✅ Request accepted! Match has been scheduled.");
        } catch (Exception e) {
            bot.sendMessage(chatId, "❌ Failed to accept request: " + e.getMessage());
        }
    }

    private void handleDeclineRequest(Long chatId, Long userId, String username, Long requestId, TelegramBot bot) {
        Player player = playerService.findOrLinkPlayer(userId, username);
        if (player == null) {
            bot.sendMessage(chatId, "You are not registered. Contact admin.");
            return;
        }

        try {
            scheduleRequestService.declineRequest(requestId, player.getId(), bot::sendMessage);
            bot.sendMessage(chatId, "❌ Request declined.");
        } catch (Exception e) {
            bot.sendMessage(chatId, "❌ Failed to decline request: " + e.getMessage());
        }
    }
}
