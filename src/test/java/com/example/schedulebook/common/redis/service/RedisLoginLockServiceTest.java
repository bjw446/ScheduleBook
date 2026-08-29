package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.consts.RedisConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisLoginLockServiceTest {

    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisLoginLockService redisLoginLockService;

    @BeforeEach
    void setUp() {

        stringRedisTemplate =
                mock(StringRedisTemplate.class);

        valueOperations =
                mock(ValueOperations.class);

        when(
                stringRedisTemplate.opsForValue()
        ).thenReturn(
                valueOperations
        );

        redisLoginLockService =
                new RedisLoginLockService(
                        stringRedisTemplate
                );
    }

    @Test
    @DisplayName("로그인 실패 횟수를 증가시키면 증가된 횟수를 반환한다")
    void givenLoginId_whenIncreaseFail_thenReturnIncreasedCount() {

        // given
        String loginId =
                "test-login";

        when(
                valueOperations.increment(
                        RedisConst.LOGIN_FAIL_PREFIX + loginId
                )
        ).thenReturn(3L);

        // when
        int result =
                redisLoginLockService.increaseFail(
                        loginId
                );

        // then
        assertEquals(
                3,
                result
        );

        verify(
                valueOperations
        ).increment(
                RedisConst.LOGIN_FAIL_PREFIX + loginId
        );

        verify(
                stringRedisTemplate
        ).expire(
                RedisConst.LOGIN_FAIL_PREFIX + loginId,
                RedisConst.LOGIN_LOCK_DURATION
        );
    }

    @Test
    @DisplayName("로그인 실패 횟수 증가 시 실패 횟수 키에 30분 TTL을 설정한다")
    void givenLoginId_whenIncreaseFail_thenSetExpiration() {

        // given
        String loginId =
                "test-login";

        when(
                valueOperations.increment(anyString())
        ).thenReturn(1L);

        // when
        redisLoginLockService.increaseFail(
                loginId
        );

        // then
        verify(
                stringRedisTemplate
        ).expire(
                RedisConst.LOGIN_FAIL_PREFIX + loginId,
                RedisConst.LOGIN_LOCK_DURATION
        );
    }

    @Test
    @DisplayName("Redis increment 결과가 null이면 실패 횟수 0을 반환한다")
    void givenNullIncrementResult_whenIncreaseFail_thenReturnZero() {

        // given
        String loginId =
                "test-login";

        when(
                valueOperations.increment(anyString())
        ).thenReturn(null);

        // when
        int result =
                redisLoginLockService.increaseFail(
                        loginId
                );

        // then
        assertEquals(
                0,
                result
        );
    }

    @Test
    @DisplayName("로그인 계정을 lock하면 LOGIN_LOCK_PREFIX가 붙은 키에 LOCK을 저장한다")
    void givenLoginId_whenLock_thenSaveLock() {

        // given
        String loginId =
                "test-login";

        String expectedKey =
                RedisConst.LOGIN_LOCK_PREFIX + loginId;

        // when
        redisLoginLockService.lock(
                loginId
        );

        // then
        verify(
                valueOperations
        ).set(
                expectedKey,
                "LOCK",
                RedisConst.LOGIN_LOCK_DURATION
        );
    }

    @Test
    @DisplayName("로그인 계정이 lock되어 있으면 true를 반환한다")
    void givenLockedLoginId_whenIsLocked_thenReturnTrue() {

        // given
        String loginId =
                "test-login";

        when(
                stringRedisTemplate.hasKey(
                        RedisConst.LOGIN_LOCK_PREFIX + loginId
                )
        ).thenReturn(true);

        // when
        boolean result =
                redisLoginLockService.isLocked(
                        loginId
                );

        // then
        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("로그인 계정이 lock되어 있지 않으면 false를 반환한다")
    void givenUnlockedLoginId_whenIsLocked_thenReturnFalse() {

        // given
        String loginId =
                "test-login";

        when(
                stringRedisTemplate.hasKey(
                        RedisConst.LOGIN_LOCK_PREFIX + loginId
                )
        ).thenReturn(false);

        // when
        boolean result =
                redisLoginLockService.isLocked(
                        loginId
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("로그인 계정의 실패 횟수와 lock 정보를 모두 삭제한다")
    void givenLoginId_whenClear_thenDeleteFailureAndLockKeys() {

        // given
        String loginId =
                "test-login";

        // when
        redisLoginLockService.clear(
                loginId
        );

        // then
        verify(
                stringRedisTemplate
        ).delete(
                RedisConst.LOGIN_FAIL_PREFIX + loginId
        );

        verify(
                stringRedisTemplate
        ).delete(
                RedisConst.LOGIN_LOCK_PREFIX + loginId
        );
    }

    @Test
    @DisplayName("로그인 실패 횟수가 최대 실패 횟수에 도달하면 계정을 lock한다")
    void givenMaxLoginFailures_whenRecordFailure_thenLock() {

        // given
        String loginId =
                "test-login";

        when(
                valueOperations.increment(
                        RedisConst.LOGIN_FAIL_PREFIX + loginId
                )
        ).thenReturn(
                (long) CommonConst.MAX_LOGIN_FAIL
        );

        // when
        redisLoginLockService.recordFailure(
                loginId
        );

        // then
        verify(
                valueOperations
        ).set(
                RedisConst.LOGIN_LOCK_PREFIX + loginId,
                "LOCK",
                RedisConst.LOGIN_LOCK_DURATION
        );
    }

    @Test
    @DisplayName("로그인 실패 횟수가 최대 실패 횟수보다 적으면 계정을 lock하지 않는다")
    void givenLessThanMaxLoginFailures_whenRecordFailure_thenNotLock() {

        // given
        String loginId =
                "test-login";

        when(
                valueOperations.increment(
                        RedisConst.LOGIN_FAIL_PREFIX + loginId
                )
        ).thenReturn(4L);

        // when
        redisLoginLockService.recordFailure(
                loginId
        );

        // then
        verify(
                valueOperations,
                never()
        ).set(
                eq(RedisConst.LOGIN_LOCK_PREFIX + loginId),
                eq("LOCK"),
                eq(RedisConst.LOGIN_LOCK_DURATION)
        );
    }
}