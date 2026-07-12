package com.example.schedulebook.common.redis;

import com.example.schedulebook.common.consts.RedisConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisSessionService {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> removeSessionScript;
    private final RedisScript<Long> deleteAllSessionsScript;

    public void addSession(Long userId, String sessionId) {
        stringRedisTemplate.opsForSet().add(
                RedisConst.USER_SESSION_PREFIX + userId,
                sessionId
        );
    }

    public void removeSession(Long userId, String sessionId) {
        stringRedisTemplate.execute(
                removeSessionScript,
                Collections.singletonList(RedisConst.USER_SESSION_PREFIX + userId),
                sessionId
        );
    }

    public Set<String> getSessions(Long userId) {
        return stringRedisTemplate.opsForSet().members(
                RedisConst.USER_SESSION_PREFIX + userId
        );
    }

    public void deleteAllSessions(Long userId) {
        stringRedisTemplate.execute(
                deleteAllSessionsScript,
                Collections.singletonList(RedisConst.USER_SESSION_PREFIX + userId),
                RedisConst.REFRESH_PREFIX
        );
    }
}
