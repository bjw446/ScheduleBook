package com.example.schedulebook.domain.auth.dto.response;

public record LoginResponse(
        String accessToken,
        Long userId,
        String nickname,
        int level
) {
}
