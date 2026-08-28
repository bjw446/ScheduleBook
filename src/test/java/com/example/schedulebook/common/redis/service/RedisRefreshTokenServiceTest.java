package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.domain.auth.enums.RefreshRotateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisRefreshTokenServiceTest {

    private StringRedisTemplate stringRedisTemplate;
    private RedisScript<Long> refreshRotateScript;
    private RedisRefreshTokenService redisRefreshTokenService;

    @BeforeEach
    void setUp() {

        stringRedisTemplate =
                mock(StringRedisTemplate.class);

        refreshRotateScript =
                mock(RedisScript.class);

        redisRefreshTokenService =
                new RedisRefreshTokenService(
                        refreshRotateScript,
                        stringRedisTemplate
                );
    }

    @Test
    @DisplayName("Refresh Token을 저장하면 sessionId 기반 key와 TTL이 적용된다")
    void givenSessionIdAndToken_whenSave_thenStoreWithExpiration() {

        // given
        String sessionId =
                "session-1";

        String refreshToken =
                "refresh-token";

        long expiration =
                3_600_000L;

        ValueOperations<String, String> valueOperations =
                mock(ValueOperations.class);

        when(
                stringRedisTemplate.opsForValue()
        ).thenReturn(
                valueOperations
        );

        // when
        redisRefreshTokenService.saveRefreshToken(
                sessionId,
                refreshToken,
                expiration
        );

        // then
        verify(
                valueOperations
        ).set(
                RedisConst.REFRESH_PREFIX + sessionId,
                refreshToken,
                Duration.ofMillis(expiration)
        );
    }

    @Test
    @DisplayName("Refresh Token을 삭제하면 sessionId 기반 key가 삭제된다")
    void givenSessionId_whenDelete_thenDeleteRefreshToken() {

        // given
        String sessionId =
                "session-1";

        // when
        redisRefreshTokenService.deleteRefreshToken(
                sessionId
        );

        // then
        verify(
                stringRedisTemplate
        ).delete(
                RedisConst.REFRESH_PREFIX + sessionId
        );
    }

    @Test
    @DisplayName("Refresh Token이 존재하면 true를 반환한다")
    void givenExistingRefreshToken_whenHasRefreshToken_thenReturnTrue() {

        // given
        String sessionId =
                "session-1";

        when(
                stringRedisTemplate.hasKey(
                        RedisConst.REFRESH_PREFIX + sessionId
                )
        ).thenReturn(true);

        // when
        boolean result =
                redisRefreshTokenService.hasRefreshToken(
                        sessionId
                );

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("Refresh Token이 존재하지 않으면 false를 반환한다")
    void givenMissingRefreshToken_whenHasRefreshToken_thenReturnFalse() {

        // given
        String sessionId =
                "session-1";

        when(
                stringRedisTemplate.hasKey(
                        RedisConst.REFRESH_PREFIX + sessionId
                )
        ).thenReturn(false);

        // when
        boolean result =
                redisRefreshTokenService.hasRefreshToken(
                        sessionId
                );

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("Refresh Token rotation 결과가 1이면 SUCCESS를 반환한다")
    void givenRotateScriptReturnsOne_whenRotate_thenReturnSuccess() {

        // given
        String sessionId =
                "session-1";

        String oldToken =
                "old-token";

        String newToken =
                "new-token";

        long expiration =
                3_600_000L;

        when(
                stringRedisTemplate.execute(
                        eq(refreshRotateScript),
                        eq(List.of(
                                RedisConst.REFRESH_PREFIX + sessionId
                        )),
                        eq(oldToken),
                        eq(newToken),
                        eq(String.valueOf(expiration))
                )
        ).thenReturn(1L);

        // when
        RefreshRotateResult result =
                redisRefreshTokenService.rotateRefreshToken(
                        sessionId,
                        oldToken,
                        newToken,
                        expiration
                );

        // then
        assertEquals(
                RefreshRotateResult.SUCCESS,
                result
        );
    }

    @Test
    @DisplayName("Refresh Token rotation 결과가 2이면 TOKEN_MISMATCH를 반환한다")
    void givenRotateScriptReturnsTwo_whenRotate_thenReturnTokenMismatch() {

        // given
        String sessionId =
                "session-1";

        String oldToken =
                "old-token";

        String newToken =
                "new-token";

        long expiration =
                3_600_000L;

        when(
                stringRedisTemplate.execute(
                        eq(refreshRotateScript),
                        eq(List.of(
                                RedisConst.REFRESH_PREFIX + sessionId
                        )),
                        eq(oldToken),
                        eq(newToken),
                        eq(String.valueOf(expiration))
                )
        ).thenReturn(2L);

        // when
        RefreshRotateResult result =
                redisRefreshTokenService.rotateRefreshToken(
                        sessionId,
                        oldToken,
                        newToken,
                        expiration
                );

        // then
        assertEquals(
                RefreshRotateResult.TOKEN_MISMATCH,
                result
        );
    }

    @Test
    @DisplayName("Refresh Token rotation 결과가 0이면 NOT_FOUND를 반환한다")
    void givenRotateScriptReturnsZero_whenRotate_thenReturnNotFound() {

        // given
        String sessionId =
                "session-1";

        when(
                stringRedisTemplate.execute(
                        eq(refreshRotateScript),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString()
                )
        ).thenReturn(0L);

        // when
        RefreshRotateResult result =
                redisRefreshTokenService.rotateRefreshToken(
                        sessionId,
                        "old-token",
                        "new-token",
                        3_600_000L
                );

        // then
        assertEquals(
                RefreshRotateResult.NOT_FOUND,
                result
        );
    }

    @Test
    @DisplayName("Refresh Token rotation 결과가 null이면 NOT_FOUND를 반환한다")
    void givenRotateScriptReturnsNull_whenRotate_thenReturnNotFound() {

        // given
        String sessionId =
                "session-1";

        when(
                stringRedisTemplate.execute(
                        eq(refreshRotateScript),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString()
                )
        ).thenReturn(null);

        // when
        RefreshRotateResult result =
                redisRefreshTokenService.rotateRefreshToken(
                        sessionId,
                        "old-token",
                        "new-token",
                        3_600_000L
                );

        // then
        assertEquals(
                RefreshRotateResult.NOT_FOUND,
                result
        );
    }

    @Test
    @DisplayName("Refresh Token key는 refresh:sessionId 형식으로 생성된다")
    void givenSessionId_whenBuildKey_thenReturnRefreshKey() {

        // given
        String sessionId =
                "session-123";

        // when
        String key =
                redisRefreshTokenService.buildRefreshTokenKey(
                        sessionId
                );

        // then
        assertEquals(
                RedisConst.REFRESH_PREFIX + sessionId,
                key
        );
    }
}