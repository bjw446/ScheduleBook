package com.example.schedulebook.common.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank
        @Size(min = 32)
        String secret,

        @NotNull
        @Positive
        Long accessTokenExpiration,

        @NotNull
        @Positive
        Long refreshTokenExpiration
) {
}