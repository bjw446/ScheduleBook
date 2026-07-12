package com.example.schedulebook.domain.auth.dto.token;

public record LoginToken(
        Long userId,
        String sessionId,
        String accessToken,
        String refreshToken
) {
}
