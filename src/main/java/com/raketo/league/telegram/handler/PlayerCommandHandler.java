package com.raketo.league.telegram.handler;

import com.raketo.league.model.Player;
import com.raketo.league.service.PlayerService;
import com.raketo.league.telegram.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class PlayerCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(PlayerCommandHandler.class);

    private final PlayerService playerService;

    public void handleCommand(Update update, TelegramBot bot) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        String username = update.getMessage().getFrom().getUserName();

        if (text.startsWith("/start")) {
            handleStartCommand(chatId, userId, username, bot);
        } else if (text.startsWith("/schedule")) {
            handleScheduleCommand(chatId, userId, bot);
        } else if (text.startsWith("/mymatches")) {
            handleMyMatchesCommand(chatId, userId, bot);
        } else if (text.startsWith("/setavailability")) {
            handleSetAvailabilityCommand(chatId, userId, bot);
        } else if (text.startsWith("/help")) {
            handleHelpCommand(chatId, bot);
        } else {
            bot.sendMessage(chatId, "Unknown command. Type /help for available commands.");
        }
    }

    public void handleCallback(Update update, TelegramBot bot) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Long userId = update.getCallbackQuery().getFrom().getId();

        logger.info("Player callback received: {}", callbackData);
    }

    private void handleStartCommand(Long chatId, Long userId, String username, TelegramBot bot) {
        Player player = playerService.findByTelegramId(userId).orElse(null);

        if (player == null) {
            bot.sendMessage(chatId, "Welcome! You are not registered yet. Please contact the administrator to join the league.");
        } else {
            bot.sendMessage(chatId, "Welcome back, " + player.getFirstName() + "! Type /help to see available commands.");
        }
    }

    private void handleScheduleCommand(Long chatId, Long userId, TelegramBot bot) {
        bot.sendMessage(chatId, "Schedule feature coming soon!");
    }

    private void handleMyMatchesCommand(Long chatId, Long userId, TelegramBot bot) {
        bot.sendMessage(chatId, "My matches feature coming soon!");
    }

    private void handleSetAvailabilityCommand(Long chatId, Long userId, TelegramBot bot) {
        bot.sendMessage(chatId, "Set availability feature coming soon! This will open the calendar web app.");
    }

    private void handleHelpCommand(Long chatId, TelegramBot bot) {
        String helpMessage = """
                Player Commands:
                /schedule - View your match schedule
                /mymatches - View your matches
                /setavailability - Set your availability (opens calendar)
                /help - Show this help message
                """;
        bot.sendMessage(chatId, helpMessage);
    }
}

