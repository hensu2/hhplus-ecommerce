package com.hhplus.ecommerce.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateTimeUtils {

    private DateTimeUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static String toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        ).toString();
    }

    public static String now() {
        return LocalDateTime.now().toString();
    }
}