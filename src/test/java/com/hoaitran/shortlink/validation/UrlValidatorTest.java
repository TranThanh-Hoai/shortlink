package com.hoaitran.shortlink.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class UrlValidatorTest {

    private UrlValidator urlValidator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        urlValidator = new UrlValidator();
        context = Mockito.mock(ConstraintValidatorContext.class);
    }

    @Test
    void testIsValid_WithValidHttpUrl_ShouldReturnTrue() {
        assertTrue(urlValidator.isValid("http://example.com", context));
        assertTrue(urlValidator.isValid("http://localhost:8080/path?query=val", context));
    }

    @Test
    void testIsValid_WithValidHttpsUrl_ShouldReturnTrue() {
        assertTrue(urlValidator.isValid("https://google.com", context));
        assertTrue(urlValidator.isValid("https://sub.domain.co.uk/some/resource", context));
    }

    @Test
    void testIsValid_WithNullOrEmpty_ShouldReturnTrue() {
        // Validation framework handles null/empty via @NotBlank/@NotNull,
        // so constraint validator should return true to avoid duplicate errors.
        assertTrue(urlValidator.isValid(null, context));
        assertTrue(urlValidator.isValid("", context));
        assertTrue(urlValidator.isValid("   ", context));
    }

    @Test
    void testIsValid_WithInvalidProtocols_ShouldReturnFalse() {
        assertFalse(urlValidator.isValid("ftp://example.com", context));
        assertFalse(urlValidator.isValid("file:///etc/passwd", context));
        assertFalse(urlValidator.isValid("javascript:alert(1)", context));
        assertFalse(urlValidator.isValid("data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==", context));
    }

    @Test
    void testIsValid_WithMalformedUrls_ShouldReturnFalse() {
        assertFalse(urlValidator.isValid("not-a-url", context));
        assertFalse(urlValidator.isValid("http://", context));
        assertFalse(urlValidator.isValid("https://   ", context));
    }
}
