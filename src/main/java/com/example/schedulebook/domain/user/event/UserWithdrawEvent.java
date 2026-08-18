package com.example.schedulebook.domain.user.event;

public record UserWithdrawEvent(
        String eventId,
        Long userId,
        String loginId
) {
}
