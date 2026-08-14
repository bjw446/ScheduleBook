package com.example.schedulebook.domain.auth.event;

public record ReplaceSessionCleanupEvent(
        Long userId,
        String oldSessionId
) {
}
