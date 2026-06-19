package com.example.schedulebook.domain.auth.dto.response;

import com.example.schedulebook.domain.user.entity.User;

public record LoginResponse(
        String accessToken,
        Long userId,
        String nickname,
        int level
) {
    public static LoginResponse from(User user, String accessToken) {
        return new LoginResponse(
                accessToken,
                user.getId(),
                user.getNickname(),
                user.getLevel()
        );
    }
}
