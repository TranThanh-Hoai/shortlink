package com.hoaitran.shortlink.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortenRequest {
    @NotBlank(message = "Original URL must not be blank")
    @URL(message = "Invalid URL format")
    private String originalUrl;

    @Size(min = 3, max = 20, message = "Custom alias must be between 3 and 20 characters")
    private String customAlias;

    private LocalDateTime expiresAt;

    public ShortenRequest(String originalUrl) {
        this.originalUrl = originalUrl;
    }
}
