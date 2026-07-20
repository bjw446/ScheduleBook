package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.consts.RedisConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisBlacklistService {
    private final StringRedisTemplate stringRedisTemplate;

    public void saveBlacklistToken(String accessToken, long expiration) {
        if (expiration <= 0) {
            return;
        }

        stringRedisTemplate.opsForValue().set(
                RedisConst.BLACKLIST_PREFIX + accessToken,
                "logout",
                Duration.ofMillis(expiration)
        );
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConst.BLACKLIST_PREFIX + accessToken));
    }
}
