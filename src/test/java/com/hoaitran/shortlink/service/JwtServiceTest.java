package com.hoaitran.shortlink.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"; // 256-bit key in Hex

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour
    }

    @Test
    void testGenerateAndExtractUsername() {
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("testuser", username);
    }

    @Test
    void testIsTokenValid() {
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        String token = jwtService.generateToken(userDetails);
        
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void testIsTokenValid_WrongUser_ShouldReturnFalse() {
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        UserDetails otherUser = User.builder()
                .username("otheruser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        String token = jwtService.generateToken(userDetails);
        
        assertFalse(jwtService.isTokenValid(token, otherUser));
    }
}
