package com.example.schedulebook.domain.user.event;

public record UserWithdrawEvent(
        Long userId,
        String loginId
) {
}
