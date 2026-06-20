package com.example.schedulebook.domain.user.dto.response;

import com.example.schedulebook.domain.user.entity.User;

public record UserResponse(
        String nickname,
        int level,
        int exp,
        int requiredExp,
        int loginCount,
        int loginStreak,
        int scheduleCount
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getNickname(),
                user.getLevel(),
                user.getExp(),
                user.getRequiredExp(),
                user.getLoginCount(),
                user.getLoginStreak(),
                user.getScheduleCount()
        );
    }
}
