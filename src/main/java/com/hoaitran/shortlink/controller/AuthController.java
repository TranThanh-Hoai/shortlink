package com.hoaitran.shortlink.controller;

import com.hoaitran.shortlink.dto.request.LoginRequest;
import com.hoaitran.shortlink.dto.request.RegisterRequest;
import com.hoaitran.shortlink.dto.response.ApiResponse;
import com.hoaitran.shortlink.dto.response.ApiResponseFactory;
import com.hoaitran.shortlink.dto.response.AuthResponse;
import com.hoaitran.shortlink.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {

        AuthResponse authResponse = authService.register(request);
        
        ApiResponse<AuthResponse> response = ApiResponseFactory.success(
                HttpStatus.CREATED,
                "User registered successfully",
                servletRequest,
                authResponse);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {

        AuthResponse authResponse = authService.login(request);

        ApiResponse<AuthResponse> response = ApiResponseFactory.success(
                HttpStatus.OK,
                "Login successful",
                servletRequest,
                authResponse);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
