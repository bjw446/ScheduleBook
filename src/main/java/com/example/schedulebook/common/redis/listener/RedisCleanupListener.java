package com.example.schedulebook.common.redis.listener;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.common.redis.service.RedisSessionService;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RedisCleanupListener {
    private final RedisSessionService redisSessionService;
    private final RedisPresenceService redisPresenceService;
    private final LoggingExecutor loggingExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserWithdrawEvent event) {
        loggingExecutor.execute("Redis Session 정리", () -> redisSessionService.deleteAllSessions(event.userId()));

        loggingExecutor.execute("Presence 삭제", () -> redisPresenceService.removeAll(event.userId()));
    }
}
