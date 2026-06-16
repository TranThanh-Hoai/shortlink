package com.hoaitran.shortlink.dto.request;

import com.hoaitran.shortlink.validation.ValidUrl;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShortenRequest {
    @NotBlank(message = "Original URL must not be blank")
    @ValidUrl
    private String url;
    private String alias;
    private LocalDateTime expiresAt;
}
