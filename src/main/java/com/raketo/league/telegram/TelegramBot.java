package com.raketo.league.telegram;

import com.raketo.league.service.AdminService;
import com.raketo.league.telegram.handler.AdminCommandHandler;
import com.raketo.league.telegram.handler.PlayerCommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final String botUsername;
    private final AdminService adminService;
    private final AdminCommandHandler adminCommandHandler;
    private final PlayerCommandHandler playerCommandHandler;

    public TelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            AdminService adminService,
            AdminCommandHandler adminCommandHandler,
            PlayerCommandHandler playerCommandHandler) {
        super(botToken);
        this.botUsername = botUsername;
        this.adminService = adminService;
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
            logger.error("Error processing update", e);
        }
    }

    private void handleTextMessage(Update update) {
        Long userId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText();
        boolean isAdmin = adminService.isAdmin(userId);

        if (isAdmin && BotCommand.isAdminCommand(text)) {
            adminCommandHandler.handleCommand(update, this);
        } else {
            playerCommandHandler.handleCommand(update, this);
        }
    }

    private void handleCallbackQuery(Update update) {
        Long userId = update.getCallbackQuery().getFrom().getId();
        String callbackData = update.getCallbackQuery().getData();
        boolean isAdmin = adminService.isAdmin(userId);

        if (isAdmin && isAdminCallback(callbackData)) {
            adminCommandHandler.handleCallback(update, this);
        } else {
            playerCommandHandler.handleCallback(update, this);
        }
    }

    private boolean isAdminCallback(String callbackData) {
        if (callbackData == null) {
            return false;
        }
        if (callbackData.startsWith("ADMIN_")) {
            return true;
        }
        return callbackData.equals("ADMIN_MENU");
    }

    private static final int MAX_MESSAGE_LENGTH = 4096;

    public void sendMessage(Long chatId, String text) {
        if (text.length() <= MAX_MESSAGE_LENGTH) {
            doSendMessage(chatId, text);
            return;
        }

        List<String> chunks = splitMessage(text, MAX_MESSAGE_LENGTH);
        for (String chunk : chunks) {
            doSendMessage(chatId, chunk);
        }
    }

    private void doSendMessage(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to chat {}", chatId, e);
        }
    }

    private List<String> splitMessage(String text, int maxLength) {
        List<String> parts = new java.util.ArrayList<>();
        while (text.length() > maxLength) {
            int splitIndex = text.lastIndexOf('\n', maxLength);
            if (splitIndex <= 0) {
                splitIndex = maxLength;
            }
            parts.add(text.substring(0, splitIndex));
            text = text.substring(splitIndex).stripLeading();
        }
        if (!text.isEmpty()) {
            parts.add(text);
        }
        return parts;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
