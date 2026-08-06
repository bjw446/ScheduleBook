package com.example.schedulebook.common.redis.delegate;

import com.example.schedulebook.common.redis.subscriber.RedisSubscriber;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterRetryService;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class NotificationRedisMessageDelegate {
    private final RedisSubscriber redisSubscriber;
    private final ObjectMapper objectMapper;
    private final DeadLetterRetryService deadLetterRetryService;

    public void handleMessage(String message) {
        try {
            NotificationEventResponse response = objectMapper.readValue(
                    message, NotificationEventResponse.class
            );

            redisSubscriber.onNotification(response);
        } catch (JsonProcessingException e) {
            log.error("Redis 메시지 역직렬화 실패, 메시지 : {}", message, e);

            deadLetterRetryService.saveDeadLetterWithRetry(
                    DeadLetterType.NOTIFICATION,
                    DeadLetterSource.NOTIFICATION_REDIS_MESSAGE_DELEGATE,
                    DeadLetterAggregateType.DESERIALIZATION_ERROR,
                    null,
                    null,
                    message,
                    e
            );

        } catch (Exception e) {
            log.error("Redis 메시지 처리 중 예상치 못한 오류 발생, 메시지 : {}", message, e);
        }
    }
}
