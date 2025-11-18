package com.raketo.league.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class FormatUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private FormatUtils() {
    }

    public static String formatHours(List<Integer> hours) {
        if (hours == null || hours.isEmpty()) return "";
        return String.format("%02d:00-%02d:59", hours.get(0), hours.get(hours.size() - 1));
    }

    public static String formatDate(java.time.LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static List<Integer> parseHoursFromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static String hoursToJson(List<Integer> hours) {
        if (hours == null || hours.isEmpty()) return "[]";
        try {
            return OBJECT_MAPPER.writeValueAsString(hours);
        } catch (Exception e) {
            return "[]";
        }
    }
}

