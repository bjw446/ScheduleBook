package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SessionBlockStore {
    private final Set<String> blockedSessions = ConcurrentHashMap.newKeySet();
    private final TaskScheduler sessionBlockTaskScheduler;

    public void block(String sessionId, long expiration) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        blockedSessions.add(sessionId);

        sessionBlockTaskScheduler.schedule(() -> blockedSessions.remove(sessionId), Instant.now().plusMillis(expiration));
    }

    public boolean isBlocked(String sessionId) {
        if (sessionId == null) {
            return false;
        }

        return blockedSessions.contains(sessionId);
    }

    public void unblock(String sessionId) {
        blockedSessions.remove(sessionId);
    }
}
