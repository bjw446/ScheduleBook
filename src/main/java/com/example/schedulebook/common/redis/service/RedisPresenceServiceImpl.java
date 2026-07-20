package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.consts.RedisConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@RequiredArgsConstructor
public class RedisPresenceServiceImpl implements RedisPresenceService{
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> presenceCountScript;
    private final RedisScript<List> presenceSessionsScript;

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

        Long count = aliveSessionCount(key);

        return count != null && count > 0;
    }

    @Override
    public int getSessionCount(Long userId) {
        String key = RedisConst.getPresenceKey(userId);

        Long count = aliveSessionCount(key);

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

        @SuppressWarnings("unchecked")
        List<String> sessions = (List<String>) stringRedisTemplate.execute(
                presenceSessionsScript,
                List.of(key),
                String.valueOf(System.currentTimeMillis())
        );

        return sessions == null ? Set.of() : new HashSet<>(sessions);
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

    @Override
    public Map<Long, Boolean> getOnlineStatuses(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        List<Object> results = stringRedisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    StringRedisConnection redis = (StringRedisConnection) connection;

                    for (Long userId : userIds) {
                        redis.eval(
                                presenceCountScript.getScriptAsString(),
                                ReturnType.INTEGER,
                                1,
                                RedisConst.getPresenceKey(userId),
                                String.valueOf(System.currentTimeMillis())
                        );
                    }

                    return null;
                }
        );

        Map<Long, Boolean> onlineMap = new HashMap<>();

        for (int i = 0; i< userIds.size(); i++) {
            Number count = (Number) results.get(i);

            onlineMap.put(userIds.get(i), count != null && count.longValue() > 0);
        }

        return onlineMap;
    }

    private Long aliveSessionCount(String key) {
        return stringRedisTemplate.execute(
                presenceCountScript,
                List.of(key),
                String.valueOf(System.currentTimeMillis())
        );
    }
}

