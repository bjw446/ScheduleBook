package com.example.schedulebook.common.redis;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.domain.auth.enums.RefreshRotateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisTokenService {
    private final RedisScript<Long> refreshRotateScript;
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

    public RefreshRotateResult rotateRefreshToken(Long userId, String oldToken, String newToken, long expiration) {
        Long result = stringRedisTemplate.execute(
                refreshRotateScript,
                List.of(RedisConst.REFRESH_PREFIX + userId),
                oldToken,
                newToken,
                String.valueOf(expiration)
        );

        if (result == null) {
            return RefreshRotateResult.NOT_FOUND;
        }

        return switch (result.intValue()) {
            case 1 -> RefreshRotateResult.SUCCESS;

            case 2 -> RefreshRotateResult.TOKEN_MISMATCH;

            default -> RefreshRotateResult.NOT_FOUND;
        };
    }

    public boolean hasRefreshToken(Long userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConst.REFRESH_PREFIX + userId));
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConst.BLACKLIST_PREFIX + accessToken));
    }
}
