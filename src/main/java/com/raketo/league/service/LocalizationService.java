package com.raketo.league.service;

import com.raketo.league.model.Language;
import com.raketo.league.model.Player;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LocalizationService {
    private final MessageSource messageSource;

    public LocalizationService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String msg(Player player, String key, Object... args) {
        Language lang;
        if (player != null) {
            lang = player.getLanguage();
        } else {
            lang = Language.RU;
        }
        return resolve(lang, key, args);
    }

    public String resolve(Language language, String key, Object... args) {
        Locale locale;
        if (language == Language.RU) {
            locale = Locale.forLanguageTag("ru");
        } else {
            locale = Locale.ENGLISH;
        }
        String text = messageSource.getMessage(key, args, null, locale);
        if (text == null) {
            text = messageSource.getMessage(key, args, key, Locale.ENGLISH);
        }
        return text;
    }
}
