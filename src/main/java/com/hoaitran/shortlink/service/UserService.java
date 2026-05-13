package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.dto.AuthResponse;
import com.hoaitran.shortlink.dto.LoginRequest;
import com.hoaitran.shortlink.dto.RegisterRequest;
import com.hoaitran.shortlink.entity.Role;
import com.hoaitran.shortlink.entity.User;
import com.hoaitran.shortlink.repository.UserRepository;
import com.hoaitran.shortlink.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(Role.USER)
                .build();

        userRepository.save(user);
        
        var jwtToken = jwtService.generateToken(new CustomUserDetails(user));
        return AuthResponse.builder()
                .accessToken(jwtToken)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var jwtToken = jwtService.generateToken(new CustomUserDetails(user));
        return AuthResponse.builder()
                .accessToken(jwtToken)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
