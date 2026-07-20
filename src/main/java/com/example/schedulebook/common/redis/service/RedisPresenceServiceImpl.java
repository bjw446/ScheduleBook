package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.consts.RedisConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;


@Service
@RequiredArgsConstructor
public class RedisPresenceServiceImpl implements RedisPresenceService{
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void register(Long userId, String sessionId) {
        String userKey = RedisConst.getPresenceKey(userId);

        long expireTime = System.currentTimeMillis() + RedisConst.PRESENCE_TTL.toMillis();

        stringRedisTemplate.opsForZSet().add(userKey, sessionId, expireTime);

        stringRedisTemplate.opsForValue().set(
                RedisConst.getPresenceSessionKey(sessionId),
                userId.toString(),
                RedisConst.PRESENCE_TTL
        );

        stringRedisTemplate.expire(userKey, RedisConst.PRESENCE_TTL);
    }

    @Override
    public void remove(Long userId, String sessionId) {
        if (userId == null) {
            return;
        }

        String userKey = RedisConst.getPresenceKey(userId);

        stringRedisTemplate.opsForZSet().remove(userKey, sessionId);

        stringRedisTemplate.delete(RedisConst.getPresenceSessionKey(sessionId));

        Long remain = stringRedisTemplate.opsForZSet().zCard(userKey);

        if (remain != null && remain == 0) {
            stringRedisTemplate.delete(userKey);
        }
    }

    @Override
    public boolean isOnline(Long userId) {
        String key = RedisConst.getPresenceKey(userId);

        cleanUpExpiredSessions(key);

        Long count = stringRedisTemplate.opsForZSet().zCard(key);

        return count != null && count > 0;
    }

    @Override
    public int getSessionCount(Long userId) {
        String key = RedisConst.getPresenceKey(userId);

        cleanUpExpiredSessions(key);

        Long count = stringRedisTemplate.opsForZSet().zCard(key);

        return count == null ? 0 : count.intValue();
    }

    @Override
    public Long findUser(String sessionId) {
        String value = stringRedisTemplate.opsForValue().get(RedisConst.getPresenceSessionKey(sessionId));

        if (value == null) {
            return null;
        }

        return Long.valueOf(value);
    }

    @Override
    public Set<String> getSessionIds(Long userId) {
        String key = RedisConst.getPresenceKey(userId);

        cleanUpExpiredSessions(key);

        Set<String> sessions = stringRedisTemplate.opsForZSet().range(key, 0, -1);

        return sessions == null ? Set.of() : sessions;
    }

    @Override
    public void refresh(Long userId, String sessionId) {
        String userKey = RedisConst.getPresenceKey(userId);

        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(userKey))) {
            return;
        }

        long expireTime = System.currentTimeMillis() + RedisConst.PRESENCE_TTL.toMillis();

        stringRedisTemplate.opsForZSet().add(userKey, sessionId, expireTime);

        stringRedisTemplate.expire(userKey, RedisConst.PRESENCE_TTL);

        stringRedisTemplate.expire(
                RedisConst.getPresenceSessionKey(sessionId),
                RedisConst.PRESENCE_TTL
        );
    }

    private void cleanUpExpiredSessions(String userKey) {
        stringRedisTemplate.opsForZSet().removeRangeByScore(userKey, 0, System.currentTimeMillis() - 1);
    }
}

