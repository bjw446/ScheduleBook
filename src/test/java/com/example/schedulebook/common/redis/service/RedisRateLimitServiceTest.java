package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisRateLimitServiceTest {

    private StringRedisTemplate stringRedisTemplate;
    private RedisScript<Long> rateLimitScript;

    private RedisRateLimitService redisRateLimitService;

    @BeforeEach
    void setUp() {

        stringRedisTemplate =
                mock(StringRedisTemplate.class);

        rateLimitScript =
                mock(RedisScript.class);

        redisRateLimitService =
                new RedisRateLimitService(
                        stringRedisTemplate,
                        rateLimitScript
                );
    }

    @Test
    @DisplayName("Lua 결과가 1이면 요청을 허용한다")
    void givenLuaResultOne_whenAllowRequest_thenReturnTrue() {

        // given
        String key =
                "rate:login:ip:127.0.0.1";

        long windowMillis =
                60000L;

        long limit =
                5L;

        String member =
                "member-1";

        when(
                stringRedisTemplate.execute(
                        eq(rateLimitScript),
                        eq(List.of(key)),
                        anyString(),
                        eq(String.valueOf(windowMillis)),
                        eq(String.valueOf(limit)),
                        eq(member)
                )
        ).thenReturn(
                1L
        );

        // when
        boolean result =
                redisRateLimitService.allowRequest(
                        key,
                        windowMillis,
                        limit,
                        member
                );

        // then
        assertTrue(
                result
        );

        verify(
                stringRedisTemplate
        ).execute(
                eq(rateLimitScript),
                eq(List.of(key)),
                anyString(),
                eq(String.valueOf(windowMillis)),
                eq(String.valueOf(limit)),
                eq(member)
        );
    }

    @Test
    @DisplayName("Lua 결과가 0이면 요청을 거부한다")
    void givenLuaResultZero_whenAllowRequest_thenReturnFalse() {

        // given
        String key =
                "rate:login:ip:127.0.0.1";

        long windowMillis =
                60000L;

        long limit =
                5L;

        String member =
                "member-1";

        when(
                stringRedisTemplate.execute(
                        eq(rateLimitScript),
                        eq(List.of(key)),
                        anyString(),
                        eq(String.valueOf(windowMillis)),
                        eq(String.valueOf(limit)),
                        eq(member)
                )
        ).thenReturn(
                0L
        );

        // when
        boolean result =
                redisRateLimitService.allowRequest(
                        key,
                        windowMillis,
                        limit,
                        member
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("Lua 결과가 null이면 요청을 거부한다")
    void givenNullLuaResult_whenAllowRequest_thenReturnFalse() {

        // given
        String key =
                "rate:login:id:test";

        long windowMillis =
                60000L;

        long limit =
                5L;

        String member =
                "member-1";

        when(
                stringRedisTemplate.execute(
                        eq(rateLimitScript),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString(),
                        eq(member)
                )
        ).thenReturn(
                null
        );

        // when
        boolean result =
                redisRateLimitService.allowRequest(
                        key,
                        windowMillis,
                        limit,
                        member
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("Redis 연결 실패 시 REDIS_UNAVAILABLE BaseException을 발생시킨다")
    void givenRedisConnectionFailure_whenAllowRequest_thenThrowRedisUnavailable() {

        // given
        String key =
                "rate:login:id:test";

        long windowMillis =
                60000L;

        long limit =
                5L;

        String member =
                "member-1";

        when(
                stringRedisTemplate.execute(
                        eq(rateLimitScript),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString(),
                        eq(member)
                )
        ).thenThrow(
                new RedisConnectionFailureException(
                        "redis unavailable"
                )
        );

        // when & then
        BaseException exception =
                assertThrows(
                        BaseException.class,
                        () -> redisRateLimitService.allowRequest(
                                key,
                                windowMillis,
                                limit,
                                member
                        )
                );

        assertEquals(
                ErrorEnum.REDIS_UNAVAILABLE,
                exception.getErrorEnum()
        );
    }
}