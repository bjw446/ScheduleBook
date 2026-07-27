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

    public void process(Long userId) {
        loggingExecutor.execute("Redis Session 정리", () -> redisSessionService.deleteAllSessions(userId));

        loggingExecutor.execute("Presence 삭제", () -> redisPresenceService.removeAll(userId));
    }
}
