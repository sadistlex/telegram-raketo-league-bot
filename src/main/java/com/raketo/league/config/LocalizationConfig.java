package com.raketo.league.config;

import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalizationConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.messages")
    public MessageSourceProperties messageSourceProperties() {
        return new MessageSourceProperties();
    }

    @Bean
    public MessageSource messageSource(MessageSourceProperties properties) {
        YamlMessageSource messageSource = new YamlMessageSource();
        messageSource.setBasename(properties.getBasename());
        messageSource.setDefaultEncoding(properties.getEncoding().name());
        messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
        messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
        if (properties.getCacheDuration() != null) {
            messageSource.setCacheMillis(properties.getCacheDuration().toMillis());
        }
        return messageSource;
    }
}

