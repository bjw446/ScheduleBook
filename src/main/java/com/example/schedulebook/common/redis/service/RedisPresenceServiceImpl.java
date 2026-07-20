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
        String userKey = RedisConst.PRESENCE_KEY(userId);

        stringRedisTemplate.opsForSet().add(userKey, sessionId);

        stringRedisTemplate.opsForValue().set(
                RedisConst.PRESENCE_SESSION_KEY(sessionId),
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

        String userKey = RedisConst.PRESENCE_KEY(userId);

        stringRedisTemplate.opsForSet().remove(userKey, sessionId);

        stringRedisTemplate.delete(RedisConst.PRESENCE_SESSION_KEY(sessionId));

        Long remain = stringRedisTemplate.opsForSet().size(userKey);

        if (remain != null && remain == 0) {
            stringRedisTemplate.delete(userKey);
        }
    }

    @Override
    public boolean isOnline(Long userId) {
        String key = RedisConst.PRESENCE_KEY(userId);

        Long count = stringRedisTemplate.opsForSet().size(key);

        return count != null && count > 0;
    }

    @Override
    public int getSessionCount(Long userId) {
        String key = RedisConst.PRESENCE_KEY(userId);

        Long count = stringRedisTemplate.opsForSet().size(key);

        return count == null ? 0 : count.intValue();
    }

    @Override
    public Long findUser(String sessionId) {
        String value = stringRedisTemplate.opsForValue().get(RedisConst.PRESENCE_SESSION_KEY(sessionId));

        if (value == null) {
            return null;
        }

        return Long.valueOf(value);
    }

    @Override
    public Set<String> getSessionIds(Long userId) {
        Set<String> sessions = stringRedisTemplate.opsForSet().members(RedisConst.PRESENCE_KEY(userId));

        return sessions == null ? Set.of() : sessions;
    }

    @Override
    public void refresh(Long userId, String sessionId) {
        String userKey = RedisConst.PRESENCE_KEY(userId);

        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(userKey))) {
            return;
        }

        stringRedisTemplate.expire(userKey, RedisConst.PRESENCE_TTL);

        stringRedisTemplate.expire(
                RedisConst.PRESENCE_SESSION_KEY(sessionId),
                RedisConst.PRESENCE_TTL
        );
    }
}

