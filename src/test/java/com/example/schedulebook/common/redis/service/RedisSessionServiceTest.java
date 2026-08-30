package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.domain.auth.dto.response.SessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisSessionServiceTest {

    private StringRedisTemplate stringRedisTemplate;

    private HashOperations<String, String, String> hashOperations;
    private SetOperations<String, String> setOperations;
    private ValueOperations<String, String> valueOperations;

    private RedisScript<Long> removeSessionScript;
    private RedisScript<Long> deleteAllSessionsScript;
    private RedisScript<Long> updateLastAccessScript;
    private RedisScript<Long> addSessionIfAvailableScript;
    private RedisScript<Long> replaceSessionIfAvailableScript;
    private RedisScript<Long> revertReplaceSessionScript;
    private RedisScript<Long> deleteReplacePendingIfOwnerScript;

    private RedisSessionService redisSessionService;

    @BeforeEach
    void setUp() {

        stringRedisTemplate =
                mock(StringRedisTemplate.class);

        hashOperations =
                mock(HashOperations.class);

        setOperations =
                mock(SetOperations.class);

        valueOperations =
                mock(ValueOperations.class);

        removeSessionScript =
                mock(RedisScript.class);

        deleteAllSessionsScript =
                mock(RedisScript.class);

        updateLastAccessScript =
                mock(RedisScript.class);

        addSessionIfAvailableScript =
                mock(RedisScript.class);

        replaceSessionIfAvailableScript =
                mock(RedisScript.class);

        revertReplaceSessionScript =
                mock(RedisScript.class);

        deleteReplacePendingIfOwnerScript =
                mock(RedisScript.class);


         // StringRedisTemplate의 opsForHash() 제네릭 문제를 피하기 위해 doReturn()을 사용한다.
        doReturn(hashOperations)
                .when(stringRedisTemplate)
                .opsForHash();

        doReturn(setOperations)
                .when(stringRedisTemplate)
                .opsForSet();

        doReturn(valueOperations)
                .when(stringRedisTemplate)
                .opsForValue();

        redisSessionService =
                new RedisSessionService(
                        stringRedisTemplate,
                        removeSessionScript,
                        deleteAllSessionsScript,
                        updateLastAccessScript,
                        addSessionIfAvailableScript,
                        replaceSessionIfAvailableScript,
                        revertReplaceSessionScript,
                        deleteReplacePendingIfOwnerScript
                );
    }

    @Test
    @DisplayName("세션을 삭제하면 removeSessionScript가 실행된다")
    void givenUserIdAndSessionId_whenRemoveSession_thenExecuteScript() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        // when
        redisSessionService.removeSession(
                userId,
                sessionId
        );

        // then
        verify(
                stringRedisTemplate
        ).execute(
                eq(removeSessionScript),
                eq(List.of(
                        RedisConst.USER_SESSION_PREFIX + userId,
                        RedisConst.SESSION_REPLACE_PENDING_PREFIX
                                + userId
                                + ":"
                                + sessionId
                )),
                eq(sessionId)
        );
    }

    @Test
    @DisplayName("사용자의 세션 목록을 조회한다")
    void givenUserId_whenGetSessions_thenReturnSessions() {

        // given
        Long userId =
                1L;

        Set<String> sessions =
                Set.of(
                        "session-1",
                        "session-2"
                );

        when(
                setOperations.members(
                        RedisConst.USER_SESSION_PREFIX + userId
                )
        ).thenReturn(
                sessions
        );

        // when
        Set<String> result =
                redisSessionService.getSessions(
                        userId
                );

        // then
        assertEquals(
                sessions,
                result
        );

        verify(
                setOperations
        ).members(
                RedisConst.USER_SESSION_PREFIX + userId
        );
    }

    @Test
    @DisplayName("모든 사용자 세션을 삭제하면 deleteAllSessionsScript가 실행된다")
    void givenUserId_whenDeleteAllSessions_thenExecuteScript() {

        // given
        Long userId =
                1L;

        // when
        redisSessionService.deleteAllSessions(
                userId
        );

        // then
        verify(
                stringRedisTemplate
        ).execute(
                eq(deleteAllSessionsScript),
                eq(List.of(
                        RedisConst.USER_SESSION_PREFIX + userId
                )),
                eq(RedisConst.REFRESH_PREFIX),
                eq(RedisConst.SESSION_INFO_PREFIX)
        );
    }

    @Test
    @DisplayName("세션 정보를 저장하면 hash에 모든 정보가 저장되고 TTL이 설정된다")
    void givenSessionInfo_whenSaveSessionInfo_thenSaveHashAndExpiration() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        String ip =
                "127.0.0.1";

        String userAgent =
                "test-agent";

        LocalDateTime loginAt =
                LocalDateTime.of(
                        2026,
                        8,
                        30,
                        10,
                        0
                );

        LocalDateTime lastAccessAt =
                LocalDateTime.of(
                        2026,
                        8,
                        30,
                        10,
                        5
                );

        SessionInfo sessionInfo =
                new SessionInfo(
                        userId,
                        sessionId,
                        ip,
                        userAgent,
                        loginAt,
                        lastAccessAt
                );

        long expiration =
                1800000L;

        String expectedKey =
                RedisConst.SESSION_INFO_PREFIX + sessionId;

        // when
        redisSessionService.saveSessionInfo(
                sessionInfo,
                expiration
        );

        // then
        verify(
                hashOperations
        ).putAll(
                eq(expectedKey),
                eq(Map.of(
                        "userId",
                        userId.toString(),
                        "ip",
                        ip,
                        "userAgent",
                        userAgent,
                        "loginAt",
                        String.valueOf(
                                loginAt
                                        .atZone(
                                                ZoneId.systemDefault()
                                        )
                                        .toInstant()
                                        .toEpochMilli()
                        ),
                        "lastAccessAt",
                        String.valueOf(
                                lastAccessAt
                                        .atZone(
                                                ZoneId.systemDefault()
                                        )
                                        .toInstant()
                                        .toEpochMilli()
                        )
                ))
        );

        verify(
                stringRedisTemplate
        ).expire(
                expectedKey,
                Duration.ofMillis(expiration)
        );
    }

    @Test
    @DisplayName("필수 정보가 모두 존재하면 세션 정보를 조회한다")
    void givenCompleteSessionInfo_whenGetSessionInfo_thenReturnSessionInfo() {

        // given
        String sessionId =
                "session-1";

        Long userId =
                1L;

        LocalDateTime loginAt =
                LocalDateTime.of(
                        2026,
                        8,
                        30,
                        10,
                        0
                );

        LocalDateTime lastAccessAt =
                LocalDateTime.of(
                        2026,
                        8,
                        30,
                        10,
                        5
                );

        long loginAtMillis =
                loginAt
                        .atZone(
                                ZoneId.systemDefault()
                        )
                        .toInstant()
                        .toEpochMilli();

        long lastAccessAtMillis =
                lastAccessAt
                        .atZone(
                                ZoneId.systemDefault()
                        )
                        .toInstant()
                        .toEpochMilli();

        Map<String, String> values =
                Map.of(
                        "userId",
                        userId.toString(),
                        "ip",
                        "127.0.0.1",
                        "userAgent",
                        "test-agent",
                        "loginAt",
                        String.valueOf(loginAtMillis),
                        "lastAccessAt",
                        String.valueOf(lastAccessAtMillis)
                );

        when(
                hashOperations.entries(
                        RedisConst.SESSION_INFO_PREFIX + sessionId
                )
        ).thenReturn(
                values
        );

        // when
        Optional<SessionInfo> result =
                redisSessionService.getSessionInfo(
                        sessionId
                );

        // then
        assertTrue(
                result.isPresent()
        );

        SessionInfo sessionInfo =
                result.get();

        assertEquals(
                userId,
                sessionInfo.userId()
        );

        assertEquals(
                sessionId,
                sessionInfo.sessionId()
        );

        assertEquals(
                "127.0.0.1",
                sessionInfo.ip()
        );

        assertEquals(
                "test-agent",
                sessionInfo.userAgent()
        );

        assertEquals(
                loginAt,
                sessionInfo.loginAt()
        );

        assertEquals(
                lastAccessAt,
                sessionInfo.lastAccessAt()
        );
    }

    @Test
    @DisplayName("세션 정보가 없으면 빈 Optional을 반환한다")
    void givenEmptySessionInfo_whenGetSessionInfo_thenReturnEmpty() {

        // given
        String sessionId =
                "session-1";

        when(
                hashOperations.entries(
                        RedisConst.SESSION_INFO_PREFIX + sessionId
                )
        ).thenReturn(
                Map.of()
        );

        // when
        Optional<SessionInfo> result =
                redisSessionService.getSessionInfo(
                        sessionId
                );

        // then
        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    @DisplayName("세션 정보의 필수 필드가 누락되면 빈 Optional을 반환한다")
    void givenIncompleteSessionInfo_whenGetSessionInfo_thenReturnEmpty() {

        // given
        String sessionId =
                "session-1";

        Map<String, String> values =
                Map.of(
                        "userId",
                        "1",
                        "ip",
                        "127.0.0.1",
                        "userAgent",
                        "test-agent"
                );

        when(
                hashOperations.entries(
                        RedisConst.SESSION_INFO_PREFIX + sessionId
                )
        ).thenReturn(
                values
        );

        // when
        Optional<SessionInfo> result =
                redisSessionService.getSessionInfo(
                        sessionId
                );

        // then
        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    @DisplayName("세션 정보를 삭제하면 session info key를 삭제한다")
    void givenSessionId_whenDeleteSessionInfo_thenDeleteKey() {

        // given
        String sessionId =
                "session-1";

        // when
        redisSessionService.deleteSessionInfo(
                sessionId
        );

        // then
        verify(
                stringRedisTemplate
        ).delete(
                RedisConst.SESSION_INFO_PREFIX + sessionId
        );
    }

    @Test
    @DisplayName("마지막 접근 시간 갱신 script가 실행된다")
    void givenSessionId_whenUpdateLastAccess_thenExecuteScript() {

        // given
        String sessionId =
                "session-1";

        // when
        redisSessionService.updateLastAccess(
                sessionId
        );

        // then
        verify(
                stringRedisTemplate
        ).execute(
                eq(updateLastAccessScript),
                eq(List.of(
                        RedisConst.SESSION_INFO_PREFIX + sessionId
                )),
                eq(String.valueOf(
                        RedisConst.LAST_ACCESS_UPDATE_INTERVAL.toMillis()
                )),
                anyString()
        );
    }

    @Test
    @DisplayName("세션 TTL을 연장하면 사용자 세션과 세션 정보의 TTL을 모두 갱신한다")
    void givenUserIdAndSessionId_whenExtendSessionTTL_thenExtendBothTTL() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        long expiration =
                1800000L;

        // when
        redisSessionService.extendSessionTTL(
                userId,
                sessionId,
                expiration
        );

        // then
        verify(
                stringRedisTemplate
        ).expire(
                RedisConst.USER_SESSION_PREFIX + userId,
                Duration.ofMillis(expiration)
        );

        verify(
                stringRedisTemplate
        ).expire(
                RedisConst.SESSION_INFO_PREFIX + sessionId,
                Duration.ofMillis(expiration)
        );
    }

    @Test
    @DisplayName("세션 정보가 존재하면 true를 반환한다")
    void givenExistingSession_whenExistsSession_thenReturnTrue() {

        // given
        String sessionId =
                "session-1";

        when(
                stringRedisTemplate.hasKey(
                        RedisConst.SESSION_INFO_PREFIX + sessionId
                )
        ).thenReturn(true);

        // when
        boolean result =
                redisSessionService.existsSession(
                        sessionId
                );

        // then
        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("세션 정보가 존재하지 않으면 false를 반환한다")
    void givenNonExistingSession_whenExistsSession_thenReturnFalse() {

        // given
        String sessionId =
                "session-1";

        when(
                stringRedisTemplate.hasKey(
                        RedisConst.SESSION_INFO_PREFIX + sessionId
                )
        ).thenReturn(false);

        // when
        boolean result =
                redisSessionService.existsSession(
                        sessionId
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("세션 제한 내에서 세션 추가가 가능하면 true를 반환한다")
    void givenAvailableSessionSlot_whenAddSessionIfAvailable_thenReturnTrue() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        int limit =
                3;

        long expiration =
                1800000L;

        when(
                stringRedisTemplate.execute(
                        eq(addSessionIfAvailableScript),
                        anyList(),
                        eq(sessionId),
                        eq(String.valueOf(limit)),
                        eq(String.valueOf(expiration))
                )
        ).thenReturn(
                1L
        );

        // when
        boolean result =
                redisSessionService.addSessionIfAvailable(
                        userId,
                        sessionId,
                        limit,
                        expiration
                );

        // then
        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("세션 제한에 도달하면 세션 추가에 실패한다")
    void givenSessionLimitReached_whenAddSessionIfAvailable_thenReturnFalse() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        int limit =
                3;

        long expiration =
                1800000L;

        when(
                stringRedisTemplate.execute(
                        eq(addSessionIfAvailableScript),
                        anyList(),
                        eq(sessionId),
                        eq(String.valueOf(limit)),
                        eq(String.valueOf(expiration))
                )
        ).thenReturn(
                0L
        );

        // when
        boolean result =
                redisSessionService.addSessionIfAvailable(
                        userId,
                        sessionId,
                        limit,
                        expiration
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("세션 교체가 가능하면 true를 반환한다")
    void givenAvailableSession_whenReplaceSessionIfAvailable_thenReturnTrue() {

        // given
        Long userId =
                1L;

        String oldSessionId =
                "old-session";

        String newSessionId =
                "new-session";

        String operationId =
                "operation-1";

        int limit =
                3;

        long sessionExpiration =
                1800000L;

        when(
                stringRedisTemplate.execute(
                        eq(replaceSessionIfAvailableScript),
                        anyList(),
                        eq(oldSessionId),
                        eq(newSessionId),
                        eq(operationId),
                        eq(String.valueOf(limit)),
                        eq(String.valueOf(
                                RedisConst.SESSION_REPLACE_PENDING_EXPIRATION
                                        .toMillis()
                        )),
                        eq(String.valueOf(sessionExpiration))
                )
        ).thenReturn(
                1L
        );

        // when
        boolean result =
                redisSessionService.replaceSessionIfAvailable(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId,
                        limit,
                        sessionExpiration
                );

        // then
        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("세션 교체가 불가능하면 false를 반환한다")
    void givenUnavailableSession_whenReplaceSessionIfAvailable_thenReturnFalse() {

        // given
        Long userId =
                1L;

        String oldSessionId =
                "old-session";

        String newSessionId =
                "new-session";

        String operationId =
                "operation-1";

        int limit =
                3;

        long sessionExpiration =
                1800000L;

        when(
                stringRedisTemplate.execute(
                        eq(replaceSessionIfAvailableScript),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                )
        ).thenReturn(
                0L
        );

        // when
        boolean result =
                redisSessionService.replaceSessionIfAvailable(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId,
                        limit,
                        sessionExpiration
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("세션 교체를 되돌리면 true를 반환한다")
    void givenValidReplacement_whenRevertReplaceSession_thenReturnTrue() {

        // given
        Long userId =
                1L;

        String oldSessionId =
                "old-session";

        String newSessionId =
                "new-session";

        String operationId =
                "operation-1";

        long sessionExpiration =
                1800000L;

        when(
                stringRedisTemplate.execute(
                        eq(revertReplaceSessionScript),
                        anyList(),
                        eq(oldSessionId),
                        eq(newSessionId),
                        eq(operationId),
                        eq(String.valueOf(sessionExpiration))
                )
        ).thenReturn(
                1L
        );

        // when
        boolean result =
                redisSessionService.revertReplaceSession(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId,
                        sessionExpiration
                );

        // then
        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("세션 교체 되돌리기에 실패하면 false를 반환한다")
    void givenInvalidReplacement_whenRevertReplaceSession_thenReturnFalse() {

        // given
        Long userId =
                1L;

        String oldSessionId =
                "old-session";

        String newSessionId =
                "new-session";

        String operationId =
                "operation-1";

        long sessionExpiration =
                1800000L;

        when(
                stringRedisTemplate.execute(
                        eq(revertReplaceSessionScript),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                )
        ).thenReturn(
                0L
        );

        // when
        boolean result =
                redisSessionService.revertReplaceSession(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId,
                        sessionExpiration
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("세션이 사용자의 세션 목록에 포함되어 있으면 true를 반환한다")
    void givenSessionMember_whenIsSessionMember_thenReturnTrue() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        when(
                setOperations.isMember(
                        RedisConst.USER_SESSION_PREFIX + userId,
                        sessionId
                )
        ).thenReturn(true);

        // when
        boolean result =
                redisSessionService.isSessionMember(
                        userId,
                        sessionId
                );

        // then
        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("세션이 사용자의 세션 목록에 포함되어 있지 않으면 false를 반환한다")
    void givenSessionNotMember_whenIsSessionMember_thenReturnFalse() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        when(
                setOperations.isMember(
                        RedisConst.USER_SESSION_PREFIX + userId,
                        sessionId
                )
        ).thenReturn(false);

        // when
        boolean result =
                redisSessionService.isSessionMember(
                        userId,
                        sessionId
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("세션 generation을 증가시키면 증가된 값을 반환한다")
    void givenUserId_whenIncrementSessionGeneration_thenReturnIncrementedGeneration() {

        // given
        Long userId =
                1L;

        when(
                valueOperations.increment(
                        RedisConst.SESSION_GENERATION_PREFIX + userId
                )
        ).thenReturn(
                2L
        );

        // when
        long result =
                redisSessionService.incrementSessionGeneration(
                        userId
                );

        // then
        assertEquals(
                2L,
                result
        );

        verify(
                valueOperations
        ).increment(
                RedisConst.SESSION_GENERATION_PREFIX + userId
        );
    }

    @Test
    @DisplayName("pending 작업의 소유자가 맞으면 pending key를 삭제하고 true를 반환한다")
    void givenOwnerOperation_whenDeleteReplacePendingIfOwner_thenReturnTrue() {

        // given
        Long userId =
                1L;

        String oldSessionId =
                "old-session";

        String operationId =
                "operation-1";

        when(
                stringRedisTemplate.execute(
                        eq(deleteReplacePendingIfOwnerScript),
                        eq(List.of(
                                RedisConst.SESSION_REPLACE_PENDING_PREFIX
                                        + userId
                                        + ":"
                                        + oldSessionId
                        )),
                        eq(operationId)
                )
        ).thenReturn(
                1L
        );

        // when
        boolean result =
                redisSessionService.deleteReplacePendingIfOwner(
                        userId,
                        oldSessionId,
                        operationId
                );

        // then
        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("pending 작업의 소유자가 아니면 false를 반환한다")
    void givenNonOwnerOperation_whenDeleteReplacePendingIfOwner_thenReturnFalse() {

        // given
        Long userId =
                1L;

        String oldSessionId =
                "old-session";

        String operationId =
                "operation-1";

        when(
                stringRedisTemplate.execute(
                        eq(deleteReplacePendingIfOwnerScript),
                        anyList(),
                        eq(operationId)
                )
        ).thenReturn(
                0L
        );

        // when
        boolean result =
                redisSessionService.deleteReplacePendingIfOwner(
                        userId,
                        oldSessionId,
                        operationId
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("RedisScript 실행 결과가 null이면 세션 추가에 실패한다")
    void givenNullScriptResult_whenAddSessionIfAvailable_thenReturnFalse() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        when(
                stringRedisTemplate.execute(
                        eq(addSessionIfAvailableScript),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString()
                )
        ).thenReturn(
                null
        );

        // when
        boolean result =
                redisSessionService.addSessionIfAvailable(
                        userId,
                        sessionId,
                        3,
                        1800000L
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("RedisScript 실행 결과가 null이면 세션 교체 되돌리기에 실패한다")
    void givenNullScriptResult_whenRevertReplaceSession_thenReturnFalse() {

        // given
        Long userId =
                1L;

        when(
                stringRedisTemplate.execute(
                        eq(revertReplaceSessionScript),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                )
        ).thenReturn(
                null
        );

        // when
        boolean result =
                redisSessionService.revertReplaceSession(
                        userId,
                        "old-session",
                        "new-session",
                        "operation-1",
                        1800000L
                );

        // then
        assertFalse(
                result
        );
    }
}