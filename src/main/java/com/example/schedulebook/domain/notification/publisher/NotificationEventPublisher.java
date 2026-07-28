package com.example.schedulebook.domain.notification.publisher;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {
    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(NotificationEventResponse response) {
        try {
            redisTemplate.convertAndSend(RedisConst.NOTIFICATION, response);

        } catch (Exception e) {
            log.error("Redis Pub/Sub 발행 실패 topic = {}, message = {}", RedisConst.NOTIFICATION, response, e);

            throw e;
        }
    }
}
