package com.example.schedulebook.common.redis.service;

import com.example.schedulebook.common.consts.RedisConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisBlacklistServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisBlacklistService redisBlacklistService;

    @BeforeEach
    void setUp() {

        redisBlacklistService =
                new RedisBlacklistService(
                        stringRedisTemplate
                );
    }

    @Test
    @DisplayName("정상적인 expiration이면 accessToken을 blacklist에 저장한다")
    void givenAccessTokenAndExpiration_whenSaveBlacklistToken_thenSaveToken() {

        // given
        String accessToken =
                "test-access-token";

        long expiration =
                60_000L;

        when(
                stringRedisTemplate.opsForValue()
        ).thenReturn(
                valueOperations
        );

        // when
        redisBlacklistService.saveBlacklistToken(
                accessToken,
                expiration
        );

        // then
        verify(
                valueOperations
        ).set(
                RedisConst.BLACKLIST_PREFIX + accessToken,
                "logout",
                Duration.ofMillis(expiration)
        );
    }

    @Test
    @DisplayName("blacklist 저장 시 Redis key는 BLACKLIST_PREFIX와 accessToken을 조합한다")
    void givenAccessToken_whenSaveBlacklistToken_thenUseBlacklistPrefix() {

        // given
        String accessToken =
                "test-access-token";

        long expiration =
                30_000L;

        when(
                stringRedisTemplate.opsForValue()
        ).thenReturn(
                valueOperations
        );

        // when
        redisBlacklistService.saveBlacklistToken(
                accessToken,
                expiration
        );

        // then
        ArgumentCaptor<String> keyCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(
                valueOperations
        ).set(
                keyCaptor.capture(),
                eq("logout"),
                eq(Duration.ofMillis(expiration))
        );

        assertEquals(
                RedisConst.BLACKLIST_PREFIX + accessToken,
                keyCaptor.getValue()
        );
    }

    @Test
    @DisplayName("expiration이 0 이하이면 blacklist를 저장하지 않는다")
    void givenNonPositiveExpiration_whenSaveBlacklistToken_thenDoNotSave() {

        // given
        String accessToken =
                "test-access-token";

        // when
        redisBlacklistService.saveBlacklistToken(
                accessToken,
                0L
        );

        // then
        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("blacklist token이 존재하면 true를 반환한다")
    void givenBlacklistedToken_whenIsBlacklisted_thenReturnTrue() {

        // given
        String accessToken =
                "test-access-token";

        when(
                stringRedisTemplate.hasKey(
                        RedisConst.BLACKLIST_PREFIX + accessToken
                )
        ).thenReturn(
                true
        );

        // when
        boolean result =
                redisBlacklistService.isBlacklisted(
                        accessToken
                );

        // then
        assertTrue(
                result
        );

        verify(
                stringRedisTemplate
        ).hasKey(
                RedisConst.BLACKLIST_PREFIX + accessToken
        );
    }

    @Test
    @DisplayName("blacklist token이 존재하지 않으면 false를 반환한다")
    void givenNonBlacklistedToken_whenIsBlacklisted_thenReturnFalse() {

        // given
        String accessToken =
                "test-access-token";

        when(
                stringRedisTemplate.hasKey(
                        RedisConst.BLACKLIST_PREFIX + accessToken
                )
        ).thenReturn(
                false
        );

        // when
        boolean result =
                redisBlacklistService.isBlacklisted(
                        accessToken
                );

        // then
        assertFalse(
                result
        );

        verify(
                stringRedisTemplate
        ).hasKey(
                RedisConst.BLACKLIST_PREFIX + accessToken
        );
    }
}