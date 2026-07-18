package com.example.schedulebook.common.redis;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
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

    public void publish(ForceLogoutSessionEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);

            stringRedisTemplate.convertAndSend(
                    RedisConst.FORCE_LOGOUT_SESSION,
                    message
            );

        } catch (JsonProcessingException e) {
            log.error("강제 로그아웃 이벤트 직렬화 실패 : {}", e.getMessage(), e);
        }
    }
}
