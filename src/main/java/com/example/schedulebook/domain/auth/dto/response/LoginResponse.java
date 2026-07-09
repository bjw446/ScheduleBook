package com.example.schedulebook.domain.auth.dto.response;

import com.example.schedulebook.domain.user.entity.User;

public record LoginResponse(
        Long userId,
        String nickname,
        int level,
        String accessToken,
        String refreshToken
) {
    public static LoginResponse from(User user, String accessToken, String refreshToken) {
        return new LoginResponse(
                user.getId(),
                user.getNickname(),
                user.getLevel(),
                accessToken,
                refreshToken
        );
    }
}
