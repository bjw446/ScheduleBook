package com.example.schedulebook.domain.auth.dto.response;

import com.example.schedulebook.domain.user.entity.User;

public record SignupResponse(
        String loginId,
        String nickname,
        String email,
        String phoneNumber
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getLoginId(),
                user.getNickname(),
                user.getEmail(),
                user.getPhoneNumber()
        );
    }
}
