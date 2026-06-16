package com.hoaitran.shortlink.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Use @NotBlank or @NotNull for null/empty checks
        }

        try {
            URL url = new URL(value);
            // Verify that URL can be converted to a URI to validate proper format
            url.toURI();

            // Enforce http/https protocol to prevent XSS (javascript:alert(1)) or other protocol issues
            String protocol = url.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                return false;
            }

            // Verify that host exists and is not empty
            String host = url.getHost();
            if (host == null || host.trim().isEmpty()) {
                return false;
            }

            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
}
