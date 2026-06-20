package com.example.schedulebook.domain.user.dto.response;

import com.example.schedulebook.domain.user.entity.User;

public record UpdateUserResponse(
        String nickname,
        String email,
        String phoneNumber
) {
    public static UpdateUserResponse from(User user) {
        return new UpdateUserResponse(
                user.getNickname(),
                user.getEmail(),
                user.getPhoneNumber()
        );
    }
}
