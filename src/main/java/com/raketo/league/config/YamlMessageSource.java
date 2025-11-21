package com.raketo.league.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.support.AbstractResourceBasedMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.text.MessageFormat;
import java.util.*;

public class YamlMessageSource extends AbstractResourceBasedMessageSource {

    private final Map<String, Map<Locale, Properties>> cachedProperties = new HashMap<>();
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        String msg = getMessageFromProperties(code, locale);
        if (msg == null) {
            return null;
        }
        return createMessageFormat(msg, locale);
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        return getMessageFromProperties(code, locale);
    }

    private String getMessageFromProperties(String code, Locale locale) {
        Properties properties = getProperties(locale);
        if (properties == null) {
            return null;
        }
        return properties.getProperty(code);
    }

    private Properties getProperties(Locale locale) {
        String basename = getBasenameSet().iterator().next();

        if (!cachedProperties.containsKey(basename)) {
            cachedProperties.put(basename, new HashMap<>());
        }

        Map<Locale, Properties> localeMap = cachedProperties.get(basename);

        if (localeMap.containsKey(locale)) {
            return localeMap.get(locale);
        }

        Properties properties = loadProperties(basename, locale);
        localeMap.put(locale, properties);

        return properties;
    }

    private Properties loadProperties(String basename, Locale locale) {
        Properties properties = new Properties();

        List<String> filenames = calculateFilenamesForLocale(basename, locale);

        for (String filename : filenames) {
            try {
                Resource resource = resolver.getResource(filename);
                if (resource.exists()) {
                    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
                    factory.setResources(resource);
                    Properties props = factory.getObject();
                    if (props != null) {
                        properties.putAll(flattenProperties("", props));
                    }
                }
            } catch (Exception e) {
            }
        }

        return properties;
    }

    private List<String> calculateFilenamesForLocale(String basename, Locale locale) {
        List<String> filenames = new ArrayList<>();

        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        if (!variant.isEmpty()) {
            filenames.add(basename + "_" + language + "_" + country + "_" + variant + ".yml");
        }
        if (!country.isEmpty()) {
            filenames.add(basename + "_" + language + "_" + country + ".yml");
        }
        if (!language.isEmpty()) {
            filenames.add(basename + "_" + language + ".yml");
        }

        filenames.add(basename + ".yml");

        Collections.reverse(filenames);
        return filenames;
    }

    private Properties flattenProperties(String prefix, Properties properties) {
        Properties result = new Properties();

        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value != null && !value.trim().isEmpty()) {
                result.setProperty(fullKey, value);
            }
        }

        return result;
    }
}

