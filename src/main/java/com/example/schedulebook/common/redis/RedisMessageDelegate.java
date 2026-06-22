package com.example.schedulebook.common.redis;

import com.example.schedulebook.domain.notification.dto.response.NotificationRealtimeResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
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
        } catch (JsonProcessingException e) {
            log.error("Redis 메시지 역직렬화 실패, 메시지 : {}", message, e);
            // TODO: 실패한 메시지를 Dead Letter Queue나 별도 저장소에 보관하여 추후 분석/재처리
        } catch (Exception e) {
            log.error("Redis 메시지 처리 중 예상치 못한 오류 발생, 메시지 : {}", message, e);
        }
    }
}
