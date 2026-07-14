package com.example.schedulebook.domain.auth.event;

public record LoginSuccessEvent(
        Long userId,
        String loginId,
        String ip,
        String userAgent
) {
}
