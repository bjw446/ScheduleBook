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
public class RedisRefreshTokenService {
    private final RedisScript<Long> refreshRotateScript;
    private final StringRedisTemplate stringRedisTemplate;

    public void saveRefreshToken(String sessionId, String refreshToken, long expiration) {
        stringRedisTemplate.opsForValue().set(
                buildRefreshTokenKey(sessionId),
                refreshToken,
                Duration.ofMillis(expiration)
        );
    }

    public void deleteRefreshToken(String sessionId) {
        stringRedisTemplate.delete(RedisConst.REFRESH_PREFIX + sessionId);
    }

    public RefreshRotateResult rotateRefreshToken(String sessionId, String oldToken, String newToken, long expiration) {
        Long result = stringRedisTemplate.execute(
                refreshRotateScript,
                List.of(RedisConst.REFRESH_PREFIX + sessionId),
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

    public boolean hasRefreshToken(String sessionId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConst.REFRESH_PREFIX + sessionId));
    }

    public String buildRefreshTokenKey(String sessionId) {
        return RedisConst.REFRESH_PREFIX + sessionId;
    }
}
