package com.hoaitran.shortlink.exception;

import com.hoaitran.shortlink.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private ResponseEntity<ApiResponse<Object>> buildErrorResponse(Exception ex, HttpStatus status, HttpServletRequest request, String message) {
        ApiResponse<Object> response = ApiResponse.builder()
                .timestamp(LocalDateTime.now())
                .code(status.value())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleAliasAlreadyExists(AliasAlreadyExistsException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request, ex.getMessage());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFound(Exception ex, HttpServletRequest request) {
        String path = "";
        if (ex instanceof NoHandlerFoundException) {
            path = ((NoHandlerFoundException) ex).getRequestURL();
        } else if (ex instanceof NoResourceFoundException) {
            path = ((NoResourceFoundException) ex).getResourcePath();
        }
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request, "The requested path does not exist: " + path);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: ", request.getRequestURI(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request, "An unexpected error occurred");
    }
}
