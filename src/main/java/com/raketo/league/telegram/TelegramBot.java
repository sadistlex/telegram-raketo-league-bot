package com.raketo.league.telegram;

import com.raketo.league.service.AdminService;
import com.raketo.league.service.PlayerService;
import com.raketo.league.telegram.handler.AdminCommandHandler;
import com.raketo.league.telegram.handler.PlayerCommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final AdminService adminService;
    private final PlayerService playerService;
    private final AdminCommandHandler adminCommandHandler;
    private final PlayerCommandHandler playerCommandHandler;

    public TelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            AdminService adminService,
            PlayerService playerService,
            AdminCommandHandler adminCommandHandler,
            PlayerCommandHandler playerCommandHandler) {
        super(botToken);
        this.botUsername = botUsername;
        this.adminService = adminService;
        this.playerService = playerService;
        this.adminCommandHandler = adminCommandHandler;
        this.playerCommandHandler = playerCommandHandler;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }

    private void handleTextMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText();

        if (adminService.isAdmin(userId)) {
            adminCommandHandler.handleCommand(update, this);
        } else {
            playerCommandHandler.handleCommand(update, this);
        }
    }

    private void handleCallbackQuery(Update update) {
        Long userId = update.getCallbackQuery().getFrom().getId();

        if (adminService.isAdmin(userId)) {
            adminCommandHandler.handleCallback(update, this);
        } else {
            playerCommandHandler.handleCallback(update, this);
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message to chat {}", chatId, e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}

