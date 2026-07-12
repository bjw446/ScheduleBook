package com.example.schedulebook.common.redis;

import com.example.schedulebook.common.consts.RedisConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisSessionService {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisRefreshTokenService redisRefreshTokenService;

    public void addSession(Long userId, String sessionId) {
        stringRedisTemplate.opsForSet().add(
                RedisConst.USER_SESSION_PREFIX + userId,
                sessionId
        );
    }

    public void removeSession(Long userId, String sessionId) {
        stringRedisTemplate.opsForSet().remove(
                RedisConst.USER_SESSION_PREFIX + userId,
                sessionId
        );

        Long size = stringRedisTemplate.opsForSet().size(
                RedisConst.USER_SESSION_PREFIX + userId
        );

        if (size != null && size == 0) {
            stringRedisTemplate.delete(RedisConst.USER_SESSION_PREFIX + userId);
        }
    }

    public Set<String> getSessions(Long userId) {
        return stringRedisTemplate.opsForSet().members(
                RedisConst.USER_SESSION_PREFIX + userId
        );
    }

    public void deleteAllSessions(Long userId) {
        Set<String> sessions = getSessions(userId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        for (String sessionId : sessions) {
            redisRefreshTokenService.deleteRefreshToken(sessionId);
        }

        stringRedisTemplate.delete(RedisConst.USER_SESSION_PREFIX + userId);
    }
}
