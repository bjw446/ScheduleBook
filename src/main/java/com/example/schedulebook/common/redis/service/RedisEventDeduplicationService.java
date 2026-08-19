package com.example.schedulebook.common.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisEventDeduplicationService {
    private final StringRedisTemplate stringRedisTemplate;
    private static final String PREFIX = "redis:event:processed:";

    public boolean isAlreadyProcessed(String eventId) {
        String key = PREFIX + eventId;

        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(
                        key,
                        "1",
                        Duration.ofHours(24)
                );

        return Boolean.FALSE.equals(result);
    }
}
