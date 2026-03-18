package com.demo.courseplatform.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ApiResponse<T>(int code, String message, T data, String timestamp) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "ok", data, now());
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "ok", null, now());
    }

    public static ApiResponse<Void> failure(int code, String message) {
        return new ApiResponse<>(code, message, null, now());
    }

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
