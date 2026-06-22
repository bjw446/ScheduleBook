package com.example.schedulebook.common.redis;

import com.example.schedulebook.domain.notification.dto.response.NotificationRealtimeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RedisMessageDelegate {
    private final RedisSubscriber redisSubscriber;
    private final ObjectMapper objectMapper;

    public void handleMessage(String message) {
        try {
            NotificationRealtimeResponse response = objectMapper.readValue(
                    message, NotificationRealtimeResponse.class
            );

            redisSubscriber.onMessage(response);
        } catch (Exception e) {
            log.error("Redis 메시지 역직렬화 실패", e);
        }
    }
}
