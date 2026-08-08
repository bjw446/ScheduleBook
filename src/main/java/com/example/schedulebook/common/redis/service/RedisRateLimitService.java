package com.example.schedulebook.common.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisRateLimitService {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> rateLimitScript;

    public boolean allowRequest(String key, long windowMillis, long limit, String member) {
        Long result = stringRedisTemplate.execute(
                rateLimitScript,
                List.of(key),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(windowMillis),
                String.valueOf(limit),
                member
        );

        return result != null && result == 1L;
    }
}