package com.hoaitran.shortlink.dto.response;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public final class ApiResponseFactory {

    private ApiResponseFactory() {
    }

    public static <T> ApiResponse<T> success(HttpStatus status, String message, HttpServletRequest request, T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .code(status.value())
                .message(message)
                .path(request.getRequestURI())
                .data(data)
                .build();
    }

    public static ApiResponse<Object> error(HttpStatus status, String message, HttpServletRequest request) {
        return ApiResponse.builder()
                .timestamp(LocalDateTime.now())
                .code(status.value())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }
}
