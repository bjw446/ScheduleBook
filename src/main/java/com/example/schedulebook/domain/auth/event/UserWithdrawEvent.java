package com.example.schedulebook.domain.auth.event;

public record UserWithdrawEvent(
        Long userId,
        String loginId,
        String ip,
        String userAgent
) {
}
