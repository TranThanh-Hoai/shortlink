package com.hoaitran.shortlink.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortenRequest {
    @NotBlank(message = "Original URL must not be blank")
    @URL(message = "Invalid URL format")
    private String originalUrl;
}
