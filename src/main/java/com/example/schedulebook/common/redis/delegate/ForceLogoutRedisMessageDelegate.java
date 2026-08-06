package com.example.schedulebook.common.redis.delegate;

import com.example.schedulebook.common.redis.subscriber.RedisSubscriber;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ForceLogoutRedisMessageDelegate {
    private final RedisSubscriber redisSubscriber;
    private final ObjectMapper objectMapper;
    private final DeadLetterService deadLetterService;

    public void handleMessage(String message) {
        try {
            ForceLogoutSessionEvent event = objectMapper.readValue(message, ForceLogoutSessionEvent.class);

            redisSubscriber.onForceLogout(event);

        } catch (JsonProcessingException e) {
            log.error("강제 로그아웃 역직렬화 실패", e);

            saveDeadLetterWithRetry(message, e);

        } catch (Exception e) {
            log.error("Redis 메시지 처리 중 예상치 못한 오류 발생, 메시지 : {}", message, e);
        }
    }

    private void saveDeadLetterWithRetry(String message, Exception e) {
        for (int i = 0; i < 3; i++) {
            try {
                deadLetterService.save(
                        DeadLetterType.FORCE_LOGOUT,
                        DeadLetterSource.FORCE_LOGOUT_REDIS_MESSAGE_DELEGATE,
                        DeadLetterAggregateType.DESERIALIZATION_ERROR,
                        null,
                        null,
                        message,
                        e.getMessage(),
                        e.getClass().getSimpleName(),
                        0
                );

                return;

            } catch (Exception exception) {
                log.warn("강제 로그아웃 DLQ 저장 재시도 {} / 3", i + 1, exception);

                try {
                    Thread.sleep((long) Math.pow(2, i) * 100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();

                    log.warn("강제 로그아웃 DLQ 저장 재시도 중 인터럽트 발생");

                    return;
                }
            }
        }

        log.error("강제 로그아웃 DLQ 저장 최종 실패, message = {}", message);
    }
}
