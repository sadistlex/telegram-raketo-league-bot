package com.raketo.league.telegram.handler;

import com.raketo.league.telegram.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminCommandHandler {

    public void handleCommand(Update update, TelegramBot bot) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if (text.startsWith("/start")) {
            handleStartCommand(chatId, bot);
        } else if (text.startsWith("/admin")) {
            handleAdminCommand(chatId, bot);
        } else if (text.startsWith("/createtournament")) {
            handleCreateTournamentCommand(chatId, bot);
        } else if (text.startsWith("/addplayer")) {
            handleAddPlayerCommand(chatId, text, bot);
        } else if (text.startsWith("/viewschedule")) {
            handleViewScheduleCommand(chatId, bot);
        } else {
            bot.sendMessage(chatId, "Unknown admin command. Type /admin for help.");
        }
    }

    public void handleCallback(Update update, TelegramBot bot) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        log.info("Admin callback received: {}", callbackData);
    }

    private void handleStartCommand(Long chatId, TelegramBot bot) {
        bot.sendMessage(chatId, "Welcome Admin! Use /admin to see available commands.");
    }

    private void handleAdminCommand(Long chatId, TelegramBot bot) {
        String helpMessage = """
                Admin Commands:
                /createtournament - Create a new tournament
                /addplayer @username - Add a player to a division
                /viewschedule - View tournament schedule
                /listtournaments - List all tournaments
                /listplayers - List all players
                """;
        bot.sendMessage(chatId, helpMessage);
    }

    private void handleCreateTournamentCommand(Long chatId, TelegramBot bot) {
        bot.sendMessage(chatId, "Tournament creation feature coming soon!");
    }

    private void handleAddPlayerCommand(Long chatId, String text, TelegramBot bot) {
        bot.sendMessage(chatId, "Add player feature coming soon!");
    }

    private void handleViewScheduleCommand(Long chatId, TelegramBot bot) {
        bot.sendMessage(chatId, "View schedule feature coming soon!");
    }
}

