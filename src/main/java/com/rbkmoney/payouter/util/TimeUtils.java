package com.rbkmoney.payouter.util;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class TimeUtils {

    public static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd_HH-mm-ss";
    public static final String DATE_FORMAT = "dd.MM.yyyy";
    private static final DateTimeFormatter FORMATTER = ISO_INSTANT;

    public static Timestamp toTimestamp(String time) {
        return Timestamp.from(Instant.from(stringToTemporal(time)));
    }


    public static String timestampToString(Timestamp timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ISO_INSTANT
                .withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(timestamp.getTime()));
    }

    public static TemporalAccessor stringToTemporal(String dateTimeStr) throws IllegalArgumentException {
        try {
            return FORMATTER.parse(dateTimeStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse: " + dateTimeStr, e);
        }
    }

    public static String formatDate(String dateString) {
        LocalDateTime parsed = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return parsed.format(formatter);
    }

    public static String formatMoscowDate(LocalDateTime localDateTime) {
        return ZonedDateTime.of(localDateTime, ZoneId.of("UTC"))
                .withZoneSameInstant(MOSCOW)
                .format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    public static ZonedDateTime toMoscowDateTime(String dateString) {
        return ZonedDateTime.parse(dateString).withZoneSameLocal(ZoneId.of("Europe/Moscow"));
    }

    public static LocalDateTime toLocalDateTime(String dateString) {
        return LocalDateTime.ofInstant(Instant.parse(dateString), ZoneOffset.UTC);
    }

    public static String toIsoInstantString(LocalDateTime localDateTime) {
        return ZonedDateTime.ofLocal(localDateTime, ZoneId.of("Z"), null).format(DateTimeFormatter.ISO_INSTANT);
    }

    public static String currentMoscowTime() {
        return DateTimeFormatter.ofPattern(DATE_TIME_FORMAT).format(Instant.now().atZone(MOSCOW));
    }

    public static String currentMoscowDate() {
        return DateTimeFormatter.ofPattern(DATE_FORMAT).format(Instant.now().atZone(MOSCOW));
    }

    public static LocalDateTime currentUTC(){
        return ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
    }
}
