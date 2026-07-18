package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SessionBlockStore {
    private final ConcurrentHashMap<String, Instant> blockedSessions = new ConcurrentHashMap<>();
    private final TaskScheduler sessionBlockTaskScheduler;

    public void block(String sessionId, long expiration) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        if (expiration <= 0) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        Instant expireAt = Instant.now().plusMillis(expiration);

        blockedSessions.put(sessionId, expireAt);

        sessionBlockTaskScheduler.schedule(() ->
                blockedSessions.remove(sessionId, expireAt),
                expireAt
        );
    }

    public boolean isBlocked(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }

        Instant expiredAt = blockedSessions.get(sessionId);

        if (expiredAt == null) {
            return false;
        }

        if (Instant.now().isAfter(expiredAt)) {
            blockedSessions.remove(sessionId);

            return false;
        }

        return true;
    }

    public void unblock(String sessionId) {
        blockedSessions.remove(sessionId);
    }
}
