package com.raketo.league.config;

import com.raketo.league.telegram.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotConfig.class);

    private final TelegramBot telegramBot;

    @Bean
    public TelegramBotsApi telegramBotsApi() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);
            logger.info("Telegram bot registered successfully: {}", telegramBot.getBotUsername());
            return botsApi;
        } catch (TelegramApiException e) {
            logger.error("Failed to register Telegram bot", e);
            throw new RuntimeException("Failed to register Telegram bot", e);
        }
    }
}

