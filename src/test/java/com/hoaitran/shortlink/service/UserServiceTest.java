package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.dto.AuthResponse;
import com.hoaitran.shortlink.dto.LoginRequest;
import com.hoaitran.shortlink.dto.RegisterRequest;
import com.hoaitran.shortlink.entity.Role;
import com.hoaitran.shortlink.entity.User;
import com.hoaitran.shortlink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        User user = User.builder()
                .username("testuser")
                .password("encodedPassword")
                .email("test@example.com")
                .role(Role.USER)
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(jwtService.generateToken(any())).thenReturn("testToken");

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("testToken", response.getAccessToken());
        assertEquals(Role.USER, response.getRole());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_UsernameExists_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.register(request));
        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        User mockUser = User.builder()
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(any())).thenReturn("testToken");

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("testToken", response.getAccessToken());
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLogin_UserNotFound_ShouldThrowException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.login(request));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testLogin_InvalidPassword_ShouldThrowException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid password"));

        assertThrows(BadCredentialsException.class, () -> userService.login(request));
    }
}
