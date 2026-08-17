package com.example.schedulebook.domain.deadletter.service;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.publisher.RedisEventPublisher;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterDeserializationRecoveryService {
    private final RedisEventPublisher redisEventPublisher;
    private final ObjectMapper objectMapper;

    public void recover(DeadLetterQueue deadLetterQueue) {
        try {
            switch (deadLetterQueue.getDeadLetterType()) {
                case FORCE_LOGOUT -> {
                    ForceLogoutSessionEvent event = objectMapper.readValue(
                            deadLetterQueue.getPayload(),
                            ForceLogoutSessionEvent.class
                    );

                    redisEventPublisher.publish(RedisConst.FORCE_LOGOUT, event);
                }

                case COMMENT -> {
                    CommentEvent event = objectMapper.readValue(
                            deadLetterQueue.getPayload(),
                            CommentEvent.class
                    );

                    redisEventPublisher.publish(
                            RedisConst.COMMENT,
                            event
                    );
                }

                case NOTIFICATION -> {
                    NotificationEventResponse event = objectMapper.readValue(
                            deadLetterQueue.getPayload(),
                            NotificationEventResponse.class
                    );

                    redisEventPublisher.publish(
                            RedisConst.NOTIFICATION,
                            event
                    );
                }

                default -> throw new BaseException(ErrorEnum.INVALID_DEAD_LETTER_TYPE);
            }

        } catch (JsonProcessingException e) {
            log.error("DeadLetter 역직렬화 복구 실패 deadLetterId = {}", deadLetterQueue.getId(), e);

            throw new BaseException(ErrorEnum.JSON_DESERIALIZATION_FAILED, e);
        }
    }
}
