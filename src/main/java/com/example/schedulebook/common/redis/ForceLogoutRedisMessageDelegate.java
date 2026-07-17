package com.example.schedulebook.common.redis;

import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ForceLogoutRedisMessageDelegate {
    private final RedisSubscriber redisSubscriber;
    private final ObjectMapper objectMapper;

    public void handleMessage(String message) {
        try {
            ForceLogoutSessionEvent event = objectMapper.readValue(message, ForceLogoutSessionEvent.class);

            redisSubscriber.onForceLogout(event);

        } catch (JsonProcessingException e) {
            log.error("강제 로그아웃 역직렬화 실패 : {}", e.getMessage(), e);

        } catch (Exception e) {
            log.error("강제 로그아웃 메시지 처리 실패 : {}", e.getMessage(), e);
        }
    }
}
