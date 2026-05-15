package com.hoaitran.shortlink.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials", ex.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "User not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidAliasException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAliasException(InvalidAliasException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Alias", ex.getMessage());
    }

    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAliasAlreadyExistsException(AliasAlreadyExistsException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Alias Already Exists", ex.getMessage());
    }

    @ExceptionHandler(LinkExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleLinkExpiredException(LinkExpiredException ex) {
        return buildResponse(HttpStatus.GONE, "Link Expired", ex.getMessage());
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLinkNotFoundException(LinkNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Link Not Found", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Runtime Error", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
