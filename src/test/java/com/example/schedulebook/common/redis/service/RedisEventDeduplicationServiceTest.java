package com.example.schedulebook.common.redis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisEventDeduplicationServiceTest {

    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;

    private RedisEventDeduplicationService redisEventDeduplicationService;

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

        redisEventDeduplicationService =
                new RedisEventDeduplicationService(
                        stringRedisTemplate
                );

        clearInvocations(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("처리되지 않은 event이면 false를 반환한다")
    void givenNewEvent_whenIsAlreadyProcessed_thenReturnFalse() {

        // given
        String eventId =
                "event-1";

        when(
                valueOperations.setIfAbsent(
                        "redis:event:processed:" + eventId,
                        "1",
                        Duration.ofHours(24)
                )
        ).thenReturn(
                true
        );

        // when
        boolean result =
                redisEventDeduplicationService.isAlreadyProcessed(
                        eventId
                );

        // then
        assertFalse(
                result
        );

        verify(
                valueOperations
        ).setIfAbsent(
                "redis:event:processed:" + eventId,
                "1",
                Duration.ofHours(24)
        );
    }

    @Test
    @DisplayName("이미 처리된 event이면 true를 반환한다")
    void givenAlreadyProcessedEvent_whenIsAlreadyProcessed_thenReturnTrue() {

        // given
        String eventId =
                "event-1";

        when(
                valueOperations.setIfAbsent(
                        "redis:event:processed:" + eventId,
                        "1",
                        Duration.ofHours(24)
                )
        ).thenReturn(
                false
        );

        // when
        boolean result =
                redisEventDeduplicationService.isAlreadyProcessed(
                        eventId
                );

        // then
        assertTrue(
                result
        );

        verify(
                valueOperations
        ).setIfAbsent(
                "redis:event:processed:" + eventId,
                "1",
                Duration.ofHours(24)
        );
    }

    @Test
    @DisplayName("Redis 결과가 null이면 이미 처리된 event가 아닌 것으로 판단한다")
    void givenNullRedisResult_whenIsAlreadyProcessed_thenReturnFalse() {

        // given
        String eventId =
                "event-1";

        when(
                valueOperations.setIfAbsent(
                        "redis:event:processed:" + eventId,
                        "1",
                        Duration.ofHours(24)
                )
        ).thenReturn(
                null
        );

        // when
        boolean result =
                redisEventDeduplicationService.isAlreadyProcessed(
                        eventId
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("event 처리 여부 확인 시 24시간 TTL을 설정한다")
    void givenEventId_whenIsAlreadyProcessed_thenSetTwentyFourHourTTL() {

        // given
        String eventId =
                "event-1";

        when(
                valueOperations.setIfAbsent(
                        anyString(),
                        eq("1"),
                        eq(Duration.ofHours(24))
                )
        ).thenReturn(
                true
        );

        // when
        redisEventDeduplicationService.isAlreadyProcessed(
                eventId
        );

        // then
        verify(
                valueOperations
        ).setIfAbsent(
                "redis:event:processed:" + eventId,
                "1",
                Duration.ofHours(24)
        );
    }
}