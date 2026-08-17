package com.example.schedulebook.common.redis.publisher;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String channel, Object event) {
        if (channel == null || channel.isBlank()) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        if (event == null) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        try {
            String message = objectMapper.writeValueAsString(event);

            stringRedisTemplate.convertAndSend(
                    channel,
                    message
            );

        } catch (JsonProcessingException e) {
            log.error("Redis 이벤트 직렬화 실패 channel = {}, eventType = {}",
                    channel,
                    event.getClass().getSimpleName(),
                    e
            );

            throw new BaseException(ErrorEnum.JSON_SERIALIZATION_FAILED, e);

        } catch (Exception e) {
            log.error("Redis Pub/Sub 발행 실패 channel = {}, eventType = {}",
                    channel,
                    event.getClass().getSimpleName(),
                    e
            );

            throw e;
        }
    }
}