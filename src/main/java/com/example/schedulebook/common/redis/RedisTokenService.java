package com.example.schedulebook.common.redis;

import com.example.schedulebook.common.consts.RedisConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisTokenService {
    private final RedisScript<Long> refreshTokenScript;
    private final StringRedisTemplate stringRedisTemplate;

    public void saveRefreshToken(Long userId, String refreshToken, long expiration) {
        stringRedisTemplate.opsForValue().set(
                RedisConst.REFRESH_PREFIX + userId,
                refreshToken,
                Duration.ofMillis(expiration)
        );
    }

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

    public void deleteRefreshToken(Long userId) {
        stringRedisTemplate.delete(RedisConst.REFRESH_PREFIX + userId);
    }

    public boolean rotateRefreshToken(Long userId, String oldToken, String newToken, long expiration) {
        Long result = stringRedisTemplate.execute(
                refreshTokenScript,
                List.of(RedisConst.REFRESH_PREFIX + userId),
                oldToken,
                newToken,
                String.valueOf(expiration)
        );

        return result != null && result == 1L;
    }

    public boolean hasRefreshToken(Long userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConst.REFRESH_PREFIX + userId));
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConst.BLACKLIST_PREFIX + accessToken));
    }
}
