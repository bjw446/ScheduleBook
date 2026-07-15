package com.example.schedulebook.common.redis;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.domain.auth.dto.response.SessionInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RedisSessionService {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> removeSessionScript;
    private final RedisScript<Long> deleteAllSessionsScript;
    private final RedisScript<Long> updateLastAccessScript;

    public void addSession(Long userId, String sessionId, long expiration) {
        stringRedisTemplate.opsForSet().add(
                buildUserSessionKey(userId),
                sessionId
        );

        stringRedisTemplate.expire(
                buildUserSessionKey(userId),
                Duration.ofMillis(expiration)
        );
    }

    public void removeSession(Long userId, String sessionId) {
        stringRedisTemplate.execute(
                removeSessionScript,
                Collections.singletonList(buildUserSessionKey(userId)),
                sessionId
        );
    }

    public Set<String> getSessions(Long userId) {
        return stringRedisTemplate.opsForSet().members(
                buildUserSessionKey(userId)
        );
    }

    public void deleteAllSessions(Long userId) {
        stringRedisTemplate.execute(
                deleteAllSessionsScript,
                Collections.singletonList(buildUserSessionKey(userId)),
                RedisConst.REFRESH_PREFIX,
                RedisConst.SESSION_INFO_PREFIX
        );
    }

    public void saveSessionInfo(SessionInfo sessionInfo, long expiration) {
        String key = buildSessionInfoKey(sessionInfo.sessionId());

        long loginAtMillis = sessionInfo.loginAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        long lastAccessAtMillis = sessionInfo.lastAccessAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        HashOperations<String, String, String> hashOperations = hashOperations();

        Map<String, String> values = Map.of(
                "userId", sessionInfo.userId().toString(),
                "ip", sessionInfo.ip(),
                "userAgent", sessionInfo.userAgent(),
                "loginAt", String.valueOf(loginAtMillis),
                "lastAccessAt", String.valueOf(lastAccessAtMillis)
        );

        hashOperations.putAll(key, values);

        stringRedisTemplate.expire(key, Duration.ofMillis(expiration));
    }

    public Optional<SessionInfo> getSessionInfo(String sessionId) {
        String key = buildSessionInfoKey(sessionId);

        Map<String, String> values = hashOperations().entries(key);

        if (values.isEmpty()) {
            return Optional.empty();
        }

        if (!values.keySet().containsAll(Set.of(
                "userId",
                "ip",
                "userAgent",
                "loginAt",
                "lastAccessAt"
        ))) {
            return Optional.empty();
        }

        return Optional.of(toSessionInfo(sessionId, values));
    }

    public void deleteSessionInfo(String sessionId) {
        stringRedisTemplate.delete(
                buildSessionInfoKey(sessionId)
        );
    }

    public void updateLastAccess(String sessionId) {
        stringRedisTemplate.execute(
                updateLastAccessScript,
                Collections.singletonList(buildSessionInfoKey(sessionId)),
                String.valueOf(RedisConst.LAST_ACCESS_UPDATE_INTERVAL.toMillis()),
                String.valueOf(System.currentTimeMillis())
        );
    }

    public void extendSessionTTL(Long userId, String sessionId, long expiration) {
        stringRedisTemplate.expire(buildUserSessionKey(userId), Duration.ofMillis(expiration));

        stringRedisTemplate.expire(buildSessionInfoKey(sessionId), Duration.ofMillis(expiration));
    }

    public boolean existsSession(String sessionId) {
        return stringRedisTemplate.hasKey(buildSessionInfoKey(sessionId));
    }

    private String buildSessionInfoKey(String sessionId) {
        return RedisConst.SESSION_INFO_PREFIX + sessionId;
    }

    private String buildUserSessionKey(Long userId) {
        return RedisConst.USER_SESSION_PREFIX + userId;
    }

    private HashOperations<String, String, String> hashOperations() {
        return stringRedisTemplate.opsForHash();
    }

    private SessionInfo toSessionInfo(String sessionId, Map<String, String> values) {
        return new SessionInfo(
                Long.parseLong(values.get("userId")),
                sessionId,
                values.get("ip"),
                values.get("userAgent"),
                toDateTime(values.get("loginAt")),
                toDateTime(values.get("lastAccessAt"))
        );
    }

    private LocalDateTime toDateTime(String value) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(Long.parseLong(value)),
                ZoneId.systemDefault()
        );
    }
}
