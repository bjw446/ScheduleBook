package com.example.schedulebook.common.redis.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.common.redis.service.RedisSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCleanupProcessor {
    private final RedisSessionService redisSessionService;
    private final RedisPresenceService redisPresenceService;
    private final LoggingExecutor loggingExecutor;

    public boolean process(Long outboxId, Long userId) {
        boolean success = true;

        success &= loggingExecutor.execute(
                outboxId,
                "Redis Session 정리",
                () -> redisSessionService.deleteAllSessions(userId)
        );

        success &= loggingExecutor.execute(
                outboxId,
                "Presence 삭제",
                () -> redisPresenceService.removeAll(userId)
        );

        return success;
    }
}