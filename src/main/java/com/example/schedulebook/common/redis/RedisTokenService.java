package com.example.schedulebook.common.redis;

import com.example.schedulebook.common.consts.RedisConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisTokenService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void saveRefreshToken(Long userId, String refreshToken, long expiration) {
        redisTemplate.opsForValue().set(
                RedisConst.REFRESH_PREFIX + userId,
                refreshToken,
                Duration.ofMillis(expiration)
        );
    }

    public void saveBlacklistToken(String accessToken, long expiration) {
        if (expiration <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(
                RedisConst.BLACKLIST_PREFIX + accessToken,
                "logout",
                Duration.ofMillis(expiration)
        );
    }

    public String findRefreshToken(Long userId) {
        Object value = redisTemplate.opsForValue().get(RedisConst.REFRESH_PREFIX + userId);

        return value == null ? null : value.toString();
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(RedisConst.REFRESH_PREFIX + userId);
    }

    public boolean hasRefreshToken(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisConst.REFRESH_PREFIX + userId));
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisConst.BLACKLIST_PREFIX + accessToken));
    }
}
