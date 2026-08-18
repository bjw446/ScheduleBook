package com.example.schedulebook.common.redis.delegate;

import com.example.schedulebook.common.redis.subscriber.RedisSubscriber;
import com.example.schedulebook.common.util.JsonMessageUtils;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterRetryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CommentRedisMessageDelegate {
    private final RedisSubscriber redisSubscriber;
    private final ObjectMapper objectMapper;
    private final DeadLetterRetryService deadLetterRetryService;

    public void handleMessage(String message) {
        try {
            CommentEvent event = objectMapper.readValue(
                    message, CommentEvent.class
            );

            redisSubscriber.onComment(event);

        } catch (JsonProcessingException e) {
            log.error("Redis 메시지 역직렬화 실패, 메시지 : {}", message, e);

            String eventId = JsonMessageUtils.extractEventId(objectMapper, message);

            deadLetterRetryService.saveDeadLetterWithRetry(
                    DeadLetterType.COMMENT,
                    DeadLetterSource.COMMENT_REDIS_MESSAGE_DELEGATE,
                    DeadLetterAggregateType.DESERIALIZATION_ERROR,
                    null,
                    null,
                    message,
                    e,
                    eventId
            );

        } catch (Exception e) {
            log.error("Redis 메시지 처리 중 예상치 못한 오류 발생, 메시지 : {}", message, e);
        }
    }
}