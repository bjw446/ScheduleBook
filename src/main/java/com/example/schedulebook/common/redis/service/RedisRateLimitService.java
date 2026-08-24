package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
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
        try {
            Long result = stringRedisTemplate.execute(
                    rateLimitScript,
                    List.of(key),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(windowMillis),
                    String.valueOf(limit),
                    member
            );

            return result != null && result == 1L;

        } catch (RedisConnectionFailureException e) {
            throw new BaseException(ErrorEnum.REDIS_UNAVAILABLE);
        }
    }
}