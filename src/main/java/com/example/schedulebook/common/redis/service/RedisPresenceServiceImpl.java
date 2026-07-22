package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.consts.RedisConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPresenceServiceImpl implements RedisPresenceService{
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> presenceCountScript;
    private final RedisScript<List> presenceSessionsScript;
    private final RedisScript<Long> presenceRefreshScript;

    @Override
    public void register(Long userId, String sessionId) {
        try {
            if (userId == null) {
                return;
            }

            String userKey = RedisConst.getPresenceKey(userId);

            long expireTime = System.currentTimeMillis() + RedisConst.PRESENCE_TTL.toMillis();

            stringRedisTemplate.opsForZSet().add(userKey, sessionId, expireTime);

            stringRedisTemplate.opsForValue().set(
                    RedisConst.getPresenceSessionKey(sessionId),
                    userId.toString(),
                    RedisConst.PRESENCE_TTL
            );

            stringRedisTemplate.expire(userKey, RedisConst.PRESENCE_TTL);

        } catch (Exception e) {
            log.warn("Redis 등록 실패", e);
        }
    }

    @Override
    public void remove(Long userId, String sessionId) {
        try {
            if (userId == null) {
                return;
            }

            String userKey = RedisConst.getPresenceKey(userId);

            stringRedisTemplate.opsForZSet().remove(userKey, sessionId);

            stringRedisTemplate.delete(RedisConst.getPresenceSessionKey(sessionId));

        } catch (Exception e) {
            log.warn("Redis 삭제 실패", e);
        }
    }

    @Override
    public boolean isOnline(Long userId) {
        try {
            String key = RedisConst.getPresenceKey(userId);

            Long count = aliveSessionCount(key);

            return count != null && count > 0;

        } catch (Exception e) {
            log.warn("Redis Presence 조회 실패", e);

            return false;
        }
    }

    @Override
    public int getSessionCount(Long userId) {
        try {
            String key = RedisConst.getPresenceKey(userId);

            Long count = aliveSessionCount(key);

            return count == null ? 0 : count.intValue();

        } catch (Exception e) {
            log.warn("Redis 조회 실패", e);

            return 0;
        }
    }

    @Override
    public Long findUser(String sessionId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(RedisConst.getPresenceSessionKey(sessionId));

            if (value == null) {
                return null;
            }

            return Long.valueOf(value);

        } catch (Exception e) {
            log.warn("Redis 조회 실패", e);

            return null;
        }
    }

    @Override
    public Set<String> getSessionIds(Long userId) {
        try {
            String key = RedisConst.getPresenceKey(userId);

            @SuppressWarnings("unchecked")
            List<String> sessions = (List<String>) stringRedisTemplate.execute(
                    presenceSessionsScript,
                    List.of(key),
                    String.valueOf(System.currentTimeMillis())
            );

            return sessions == null ? Set.of() : new HashSet<>(sessions);

        } catch (Exception e) {
            log.error("Redis 조회 실패", e);

            return Set.of();
        }
    }

    @Override
    public void refresh(Long userId, String sessionId) {
        try {
            String userKey = RedisConst.getPresenceKey(userId);

            String sessionKey = RedisConst.getPresenceSessionKey(sessionId);

            long expireTime = System.currentTimeMillis() + RedisConst.PRESENCE_TTL.toMillis();

            Long updated = stringRedisTemplate.execute(
                    presenceRefreshScript,
                    List.of(userKey, sessionKey),
                    sessionId,
                    String.valueOf(expireTime),
                    String.valueOf(RedisConst.PRESENCE_TTL.toMillis())
            );

            if (updated != null && updated == 0L) {
                log.debug("이미 삭제된 세션 heartbeat 무시 sessionId = {}", sessionId);
            }

        } catch (Exception e) {
            log.warn("Redis 리프레쉬 실패", e);
        }
    }

    @Override
    public Map<Long, Boolean> getOnlineStatuses(List<Long> userIds) {
        try {
            if (userIds.isEmpty()) {
                return Map.of();
            }

            List<Object> results = stringRedisTemplate.executePipelined(
                    (RedisCallback<Object>) connection -> {
                        StringRedisConnection redis = (StringRedisConnection) connection;

                        String scriptAsString = presenceCountScript.getScriptAsString();

                        String now = String.valueOf(System.currentTimeMillis());

                        for (Long userId : userIds) {
                            redis.eval(
                                    scriptAsString,
                                    ReturnType.INTEGER,
                                    1,
                                    RedisConst.getPresenceKey(userId),
                                    now
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

        } catch (Exception e) {
            log.error("Redis 조회 실패", e);

            return Map.of();
        }
    }

    private Long aliveSessionCount(String key) {
        return stringRedisTemplate.execute(
                presenceCountScript,
                List.of(key),
                String.valueOf(System.currentTimeMillis())
        );
    }
}

