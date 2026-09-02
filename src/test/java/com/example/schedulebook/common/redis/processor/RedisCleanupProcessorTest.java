package com.example.schedulebook.common.redis.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.common.redis.service.RedisSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCleanupProcessorTest {

    @Mock
    private RedisSessionService redisSessionService;

    @Mock
    private RedisPresenceService redisPresenceService;

    @Mock
    private LoggingExecutor loggingExecutor;

    private RedisCleanupProcessor redisCleanupProcessor;

    private final Long outboxId = 1L;
    private final Long userId = 100L;

    @BeforeEach
    void setUp() {
        redisCleanupProcessor = new RedisCleanupProcessor(
                redisSessionService,
                redisPresenceService,
                loggingExecutor
        );
    }

    @Test
    void 세션_정리_작업을_LoggingExecutor에_위임한다() {
        // given
        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        )).thenReturn(true);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        )).thenReturn(true);

        // when
        redisCleanupProcessor.process(outboxId, userId);

        // then
        verify(loggingExecutor).execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        );
    }

    @Test
    void Redis_Session_정리_Runnable에_userId가_전달된다() {
        // given
        ArgumentCaptor<Runnable> runnableCaptor =
                ArgumentCaptor.forClass(Runnable.class);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        )).thenReturn(true);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        )).thenReturn(true);

        // when
        redisCleanupProcessor.process(outboxId, userId);

        // then
        verify(loggingExecutor).execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                runnableCaptor.capture()
        );

        runnableCaptor.getValue().run();

        verify(redisSessionService).deleteAllSessions(userId);
    }

    @Test
    void Presence_정리_작업을_LoggingExecutor에_위임한다() {
        // given
        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        )).thenReturn(true);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        )).thenReturn(true);

        // when
        redisCleanupProcessor.process(outboxId, userId);

        // then
        verify(loggingExecutor).execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        );
    }

    @Test
    void Presence_삭제_Runnable에_userId가_전달된다() {
        // given
        ArgumentCaptor<Runnable> runnableCaptor =
                ArgumentCaptor.forClass(Runnable.class);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        )).thenReturn(true);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        )).thenReturn(true);

        // when
        redisCleanupProcessor.process(outboxId, userId);

        // then
        verify(loggingExecutor).execute(
                eq(outboxId),
                eq("Presence 삭제"),
                runnableCaptor.capture()
        );

        runnableCaptor.getValue().run();

        verify(redisPresenceService).removeAll(userId);
    }

    @Test
    void 두_작업이_모두_성공하면_true를_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        )).thenReturn(true);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        )).thenReturn(true);

        // when
        boolean result = redisCleanupProcessor.process(outboxId, userId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void Session_정리가_실패하면_false를_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        )).thenReturn(false);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        )).thenReturn(true);

        // when
        boolean result = redisCleanupProcessor.process(outboxId, userId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void Presence_정리가_실패하면_false를_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        )).thenReturn(true);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        )).thenReturn(false);

        // when
        boolean result = redisCleanupProcessor.process(outboxId, userId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void Session_정리가_실패해도_Presence_정리를_수행한다() {
        // given
        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        )).thenReturn(false);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        )).thenReturn(true);

        // when
        redisCleanupProcessor.process(outboxId, userId);

        // then
        verify(loggingExecutor).execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        );

        verify(loggingExecutor).execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        );
    }

    @Test
    void 두_작업이_모두_실패하면_false를_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        )).thenReturn(false);

        when(loggingExecutor.execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        )).thenReturn(false);

        // when
        boolean result = redisCleanupProcessor.process(outboxId, userId);

        // then
        assertThat(result).isFalse();

        verify(loggingExecutor).execute(
                eq(outboxId),
                eq("Redis Session 정리"),
                any(Runnable.class)
        );

        verify(loggingExecutor).execute(
                eq(outboxId),
                eq("Presence 삭제"),
                any(Runnable.class)
        );
    }
}