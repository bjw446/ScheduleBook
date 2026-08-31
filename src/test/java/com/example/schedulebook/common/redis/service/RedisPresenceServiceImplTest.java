package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.consts.RedisConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisPresenceServiceImplTest {

    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    private RedisScript<Long> presenceCountScript;
    private RedisScript<List> presenceSessionsScript;
    private RedisScript<Long> presenceRefreshScript;
    private RedisScript<Long> presenceRemoveScript;
    private RedisScript<Long> presenceRegisterScript;
    private RedisScript<Long> deleteAllPresenceScript;

    private RedisPresenceServiceImpl redisPresenceService;

    @BeforeEach
    void setUp() {

        stringRedisTemplate =
                mock(StringRedisTemplate.class);

        valueOperations =
                mock(ValueOperations.class);

        presenceCountScript =
                mock(RedisScript.class);

        presenceSessionsScript =
                mock(RedisScript.class);

        presenceRefreshScript =
                mock(RedisScript.class);

        presenceRemoveScript =
                mock(RedisScript.class);

        presenceRegisterScript =
                mock(RedisScript.class);

        deleteAllPresenceScript =
                mock(RedisScript.class);

        when(
                stringRedisTemplate.opsForValue()
        ).thenReturn(
                valueOperations
        );

        redisPresenceService =
                new RedisPresenceServiceImpl(
                        stringRedisTemplate,
                        presenceCountScript,
                        presenceSessionsScript,
                        presenceRefreshScript,
                        presenceRemoveScript,
                        presenceRegisterScript,
                        deleteAllPresenceScript
                );

        // setUp()에서 발생한 Mockito interaction 기록 제거
        clearInvocations(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("유저가 Presence에 등록되면 register script를 실행한다")
    void givenValidUserAndSession_whenRegister_thenExecuteScript() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        // when
        redisPresenceService.register(
                userId,
                sessionId
        );

        // then
        verify(
                stringRedisTemplate
        ).execute(
                eq(presenceRegisterScript),
                eq(List.of(
                        RedisConst.getPresenceKey(userId),
                        RedisConst.getPresenceSessionKey(sessionId)
                )),
                eq(sessionId),
                eq(String.valueOf(userId)),
                anyString(),
                eq(String.valueOf(
                        RedisConst.PRESENCE_TTL.toMillis()
                ))
        );
    }

    @Test
    @DisplayName("userId가 null이면 Presence 등록을 수행하지 않는다")
    void givenNullUserId_whenRegister_thenDoNothing() {

        // given
        Long userId =
                null;

        String sessionId =
                "session-1";

        // when
        redisPresenceService.register(
                userId,
                sessionId
        );

        // then
        verify(
                stringRedisTemplate,
                never()
        ).execute(
                eq(presenceRegisterScript),
                anyList(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("sessionId가 null이면 Presence 등록을 수행하지 않는다")
    void givenNullSessionId_whenRegister_thenDoNothing() {

        // given
        Long userId =
                1L;

        String sessionId =
                null;

        // when
        redisPresenceService.register(
                userId,
                sessionId
        );

        // then
        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("sessionId가 빈 문자열이면 Presence 등록을 수행하지 않는다")
    void givenBlankSessionId_whenRegister_thenDoNothing() {

        // given
        Long userId =
                1L;

        String sessionId =
                " ";

        // when
        redisPresenceService.register(
                userId,
                sessionId
        );

        // then
        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("Presence 등록 중 Redis 예외가 발생해도 예외를 전파하지 않는다")
    void givenRedisException_whenRegister_thenNotPropagateException() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        when(
                stringRedisTemplate.execute(
                        eq(presenceRegisterScript),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                )
        ).thenThrow(
                new RuntimeException("redis error")
        );

        // when & then
        assertDoesNotThrow(
                () -> redisPresenceService.register(
                        userId,
                        sessionId
                )
        );
    }

    @Test
    @DisplayName("유저의 세션을 제거하면 remove script를 실행한다")
    void givenValidUserAndSession_whenRemove_thenExecuteScript() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        // when
        redisPresenceService.remove(
                userId,
                sessionId
        );

        // then
        verify(
                stringRedisTemplate
        ).execute(
                eq(presenceRemoveScript),
                eq(List.of(
                        RedisConst.getPresenceKey(userId),
                        RedisConst.getPresenceSessionKey(sessionId)
                )),
                eq(sessionId)
        );
    }

    @Test
    @DisplayName("sessionId가 null이면 세션 제거를 수행하지 않는다")
    void givenNullSessionId_whenRemove_thenDoNothing() {

        // given
        Long userId =
                1L;

        String sessionId =
                null;

        // when
        redisPresenceService.remove(
                userId,
                sessionId
        );

        // then
        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("세션 제거 중 Redis 예외가 발생해도 예외를 전파하지 않는다")
    void givenRedisException_whenRemove_thenNotPropagateException() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        when(
                stringRedisTemplate.execute(
                        eq(presenceRemoveScript),
                        anyList(),
                        eq(sessionId)
                )
        ).thenThrow(
                new RuntimeException("redis error")
        );

        // when & then
        assertDoesNotThrow(
                () -> redisPresenceService.remove(
                        userId,
                        sessionId
                )
        );
    }

    @Test
    @DisplayName("활성 세션이 하나 이상 존재하면 온라인 상태로 판단한다")
    void givenAliveSession_whenIsOnline_thenReturnTrue() {

        // given
        Long userId =
                1L;

        when(
                stringRedisTemplate.execute(
                        eq(presenceCountScript),
                        anyList(),
                        anyString()
                )
        ).thenReturn(
                1L
        );

        // when
        boolean result =
                redisPresenceService.isOnline(
                        userId
                );

        // then
        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("활성 세션이 없으면 오프라인 상태로 판단한다")
    void givenNoAliveSession_whenIsOnline_thenReturnFalse() {

        // given
        Long userId =
                1L;

        when(
                stringRedisTemplate.execute(
                        eq(presenceCountScript),
                        anyList(),
                        anyString()
                )
        ).thenReturn(
                0L
        );

        // when
        boolean result =
                redisPresenceService.isOnline(
                        userId
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("Presence count가 null이면 오프라인 상태로 판단한다")
    void givenNullPresenceCount_whenIsOnline_thenReturnFalse() {

        // given
        Long userId =
                1L;

        when(
                stringRedisTemplate.execute(
                        eq(presenceCountScript),
                        anyList(),
                        anyString()
                )
        ).thenReturn(
                null
        );

        // when
        boolean result =
                redisPresenceService.isOnline(
                        userId
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("userId가 null이면 오프라인 상태를 반환한다")
    void givenNullUserId_whenIsOnline_thenReturnFalse() {

        // when
        boolean result =
                redisPresenceService.isOnline(
                        null
                );

        // then
        assertFalse(
                result
        );

        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("온라인 세션 개수를 반환한다")
    void givenAliveSessions_whenGetSessionCount_thenReturnCount() {

        // given
        Long userId =
                1L;

        when(
                stringRedisTemplate.execute(
                        eq(presenceCountScript),
                        anyList(),
                        anyString()
                )
        ).thenReturn(
                3L
        );

        // when
        int result =
                redisPresenceService.getSessionCount(
                        userId
                );

        // then
        assertEquals(
                3,
                result
        );
    }

    @Test
    @DisplayName("Presence count가 null이면 세션 개수 0을 반환한다")
    void givenNullPresenceCount_whenGetSessionCount_thenReturnZero() {

        // given
        Long userId =
                1L;

        when(
                stringRedisTemplate.execute(
                        eq(presenceCountScript),
                        anyList(),
                        anyString()
                )
        ).thenReturn(
                null
        );

        // when
        int result =
                redisPresenceService.getSessionCount(
                        userId
                );

        // then
        assertEquals(
                0,
                result
        );
    }

    @Test
    @DisplayName("userId가 null이면 세션 개수 0을 반환한다")
    void givenNullUserId_whenGetSessionCount_thenReturnZero() {

        // when
        int result =
                redisPresenceService.getSessionCount(
                        null
                );

        // then
        assertEquals(
                0,
                result
        );

        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("세션 ID로 사용자 ID를 조회한다")
    void givenSessionId_whenFindUser_thenReturnUserId() {

        // given
        String sessionId =
                "session-1";

        when(
                valueOperations.get(
                        RedisConst.getPresenceSessionKey(sessionId)
                )
        ).thenReturn(
                "1"
        );

        // when
        Long result =
                redisPresenceService.findUser(
                        sessionId
                );

        // then
        assertEquals(
                1L,
                result
        );

        verify(
                valueOperations
        ).get(
                RedisConst.getPresenceSessionKey(sessionId)
        );
    }

    @Test
    @DisplayName("세션 ID가 존재하지 않으면 null을 반환한다")
    void givenNonExistingSession_whenFindUser_thenReturnNull() {

        // given
        String sessionId =
                "session-1";

        when(
                valueOperations.get(
                        RedisConst.getPresenceSessionKey(sessionId)
                )
        ).thenReturn(
                null
        );

        // when
        Long result =
                redisPresenceService.findUser(
                        sessionId
                );

        // then
        assertNull(
                result
        );
    }

    @Test
    @DisplayName("sessionId가 null이면 null을 반환한다")
    void givenNullSessionId_whenFindUser_thenReturnNull() {

        // when
        Long result =
                redisPresenceService.findUser(
                        null
                );

        // then
        assertNull(
                result
        );

        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("사용자의 활성 세션 ID 목록을 조회한다")
    void givenUserId_whenGetSessionIds_thenReturnSessionIds() {

        // given
        Long userId =
                1L;

        List<String> sessions =
                List.of(
                        "session-1",
                        "session-2"
                );

        when(
                stringRedisTemplate.execute(
                        eq(presenceSessionsScript),
                        anyList(),
                        anyString()
                )
        ).thenReturn(
                sessions
        );

        // when
        Set<String> result =
                redisPresenceService.getSessionIds(
                        userId
                );

        // then
        assertEquals(
                Set.of(
                        "session-1",
                        "session-2"
                ),
                result
        );
    }

    @Test
    @DisplayName("세션 목록 조회 결과가 null이면 빈 Set을 반환한다")
    void givenNullSessionList_whenGetSessionIds_thenReturnEmptySet() {

        // given
        Long userId =
                1L;

        when(
                stringRedisTemplate.execute(
                        eq(presenceSessionsScript),
                        anyList(),
                        anyString()
                )
        ).thenReturn(
                null
        );

        // when
        Set<String> result =
                redisPresenceService.getSessionIds(
                        userId
                );

        // then
        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    @DisplayName("userId가 null이면 빈 Set을 반환한다")
    void givenNullUserId_whenGetSessionIds_thenReturnEmptySet() {

        // when
        Set<String> result =
                redisPresenceService.getSessionIds(
                        null
                );

        // then
        assertTrue(
                result.isEmpty()
        );

        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("세션 목록 조회 중 Redis 예외가 발생하면 빈 Set을 반환한다")
    void givenRedisException_whenGetSessionIds_thenReturnEmptySet() {

        // given
        Long userId =
                1L;

        when(
                stringRedisTemplate.execute(
                        eq(presenceSessionsScript),
                        anyList(),
                        anyString()
                )
        ).thenThrow(
                new RuntimeException("redis error")
        );

        // when
        Set<String> result =
                redisPresenceService.getSessionIds(
                        userId
                );

        // then
        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    @DisplayName("Presence heartbeat를 갱신하면 refresh script를 실행한다")
    void givenValidUserAndSession_whenRefresh_thenExecuteScript() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        // when
        redisPresenceService.refresh(
                userId,
                sessionId
        );

        // then
        verify(
                stringRedisTemplate
        ).execute(
                eq(presenceRefreshScript),
                eq(List.of(
                        RedisConst.getPresenceKey(userId),
                        RedisConst.getPresenceSessionKey(sessionId)
                )),
                eq(sessionId),
                anyString(),
                eq(String.valueOf(
                        RedisConst.PRESENCE_TTL.toMillis()
                ))
        );
    }

    @Test
    @DisplayName("userId가 null이면 heartbeat 갱신을 수행하지 않는다")
    void givenNullUserId_whenRefresh_thenDoNothing() {

        // when
        redisPresenceService.refresh(
                null,
                "session-1"
        );

        // then
        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("sessionId가 null이면 heartbeat 갱신을 수행하지 않는다")
    void givenNullSessionId_whenRefresh_thenDoNothing() {

        // when
        redisPresenceService.refresh(
                1L,
                null
        );

        // then
        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("heartbeat 갱신 중 Redis 예외가 발생해도 예외를 전파하지 않는다")
    void givenRedisException_whenRefresh_thenNotPropagateException() {

        // given
        Long userId =
                1L;

        String sessionId =
                "session-1";

        when(
                stringRedisTemplate.execute(
                        eq(presenceRefreshScript),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString()
                )
        ).thenThrow(
                new RuntimeException("redis error")
        );

        // when & then
        assertDoesNotThrow(
                () -> redisPresenceService.refresh(
                        userId,
                        sessionId
                )
        );
    }

    @Test
    @DisplayName("여러 사용자의 온라인 상태를 조회한다")
    void givenUserIds_whenGetOnlineStatuses_thenReturnStatuses() {

        // given
        List<Long> userIds =
                List.of(
                        1L,
                        2L,
                        3L
                );

        when(
                stringRedisTemplate.executePipelined(
                        any(RedisCallback.class)
                )
        ).thenReturn(
                List.of(
                        1L,
                        0L,
                        2L
                )
        );

        // when
        Map<Long, Boolean> result =
                redisPresenceService.getOnlineStatuses(
                        userIds
                );

        // then
        assertEquals(
                Map.of(
                        1L, true,
                        2L, false,
                        3L, true
                ),
                result
        );

        verify(
                stringRedisTemplate
        ).executePipelined(
                any(RedisCallback.class)
        );
    }

    @Test
    @DisplayName("사용자 ID 목록이 null이면 빈 Map을 반환한다")
    void givenNullUserIds_whenGetOnlineStatuses_thenReturnEmptyMap() {

        // when
        Map<Long, Boolean> result =
                redisPresenceService.getOnlineStatuses(
                        null
                );

        // then
        assertTrue(
                result.isEmpty()
        );

        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("사용자 ID 목록이 비어 있으면 빈 Map을 반환한다")
    void givenEmptyUserIds_whenGetOnlineStatuses_thenReturnEmptyMap() {

        // when
        Map<Long, Boolean> result =
                redisPresenceService.getOnlineStatuses(
                        List.of()
                );

        // then
        assertTrue(
                result.isEmpty()
        );

        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("여러 사용자 온라인 상태 조회 중 Redis 예외가 발생하면 빈 Map을 반환한다")
    void givenRedisException_whenGetOnlineStatuses_thenReturnEmptyMap() {

        // given
        List<Long> userIds =
                List.of(
                        1L,
                        2L
                );

        when(
                stringRedisTemplate.executePipelined(
                        any(RedisCallback.class)
                )
        ).thenThrow(
                new RuntimeException("redis error")
        );

        // when
        Map<Long, Boolean> result =
                redisPresenceService.getOnlineStatuses(
                        userIds
                );

        // then
        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    @DisplayName("특정 사용자의 Presence 전체를 삭제하면 deleteAllPresenceScript를 실행한다")
    void givenUserId_whenRemoveAll_thenExecuteScript() {

        // given
        Long userId =
                1L;

        // when
        redisPresenceService.removeAll(
                userId
        );

        // then
        verify(
                stringRedisTemplate
        ).execute(
                eq(deleteAllPresenceScript),
                eq(List.of(
                        RedisConst.getPresenceKey(userId)
                )),
                eq(RedisConst.PRESENCE_SESSION)
        );
    }

    @Test
    @DisplayName("userId가 null이면 Presence 전체 삭제를 수행하지 않는다")
    void givenNullUserId_whenRemoveAll_thenDoNothing() {

        // when
        redisPresenceService.removeAll(
                null
        );

        // then
        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("Presence 전체 삭제 중 Redis 예외가 발생해도 예외를 전파하지 않는다")
    void givenRedisException_whenRemoveAll_thenNotPropagateException() {

        // given
        Long userId =
                1L;

        when(
                stringRedisTemplate.execute(
                        eq(deleteAllPresenceScript),
                        anyList(),
                        eq(RedisConst.PRESENCE_SESSION)
                )
        ).thenThrow(
                new RuntimeException("redis error")
        );

        // when & then
        assertDoesNotThrow(
                () -> redisPresenceService.removeAll(
                        userId
                )
        );
    }
}