package com.example.schedulebook.domain.schedule.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.schedule.service.ScheduleService;
import com.example.schedulebook.domain.scheduleshare.service.ScheduleShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleCleanupProcessorTest {

    private static final Long OUTBOX_ID = 100L;
    private static final Long USER_ID = 1L;

    @Mock
    private ScheduleShareService scheduleShareService;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private LoggingExecutor loggingExecutor;

    private ScheduleCleanupProcessor scheduleCleanupProcessor;

    @BeforeEach
    void setUp() {
        scheduleCleanupProcessor = new ScheduleCleanupProcessor(
                scheduleShareService,
                scheduleService,
                loggingExecutor
        );
    }

    @Test
    void 일정_정리_두_작업이_모두_성공하면_true를_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("공유 받은 일정 및 공유 한 일정 삭제"),
                any(Runnable.class)
        )).thenAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        });

        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("일정 참여자 및 일정 삭제"),
                any(Runnable.class)
        )).thenAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        });

        // when
        boolean result = scheduleCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        assertThat(result)
                .isTrue();

        verify(scheduleShareService)
                .deleteAllShared(USER_ID);

        verify(scheduleService)
                .deleteAllSchedules(USER_ID);

        verify(loggingExecutor, times(2))
                .execute(
                        eq(OUTBOX_ID),
                        anyString(),
                        any(Runnable.class)
                );
    }

    @Test
    void 공유_일정_삭제가_실패해도_일정_삭제를_계속_수행하고_false를_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("공유 받은 일정 및 공유 한 일정 삭제"),
                any(Runnable.class)
        )).thenAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);

            try {
                action.run();
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("일정 참여자 및 일정 삭제"),
                any(Runnable.class)
        )).thenAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);

            try {
                action.run();
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        doThrow(new RuntimeException("공유 일정 삭제 실패"))
                .when(scheduleShareService)
                .deleteAllShared(USER_ID);

        // when
        boolean result = scheduleCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        assertThat(result)
                .isFalse();

        verify(scheduleShareService)
                .deleteAllShared(USER_ID);

        verify(scheduleService)
                .deleteAllSchedules(USER_ID);

        verify(loggingExecutor, times(2))
                .execute(
                        eq(OUTBOX_ID),
                        anyString(),
                        any(Runnable.class)
                );
    }

    @Test
    void 일정_삭제가_실패하면_false를_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("공유 받은 일정 및 공유 한 일정 삭제"),
                any(Runnable.class)
        )).thenAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        });

        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("일정 참여자 및 일정 삭제"),
                any(Runnable.class)
        )).thenAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);

            try {
                action.run();
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        doThrow(new RuntimeException("일정 삭제 실패"))
                .when(scheduleService)
                .deleteAllSchedules(USER_ID);

        // when
        boolean result = scheduleCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        assertThat(result)
                .isFalse();

        verify(scheduleShareService)
                .deleteAllShared(USER_ID);

        verify(scheduleService)
                .deleteAllSchedules(USER_ID);

        verify(loggingExecutor, times(2))
                .execute(
                        eq(OUTBOX_ID),
                        anyString(),
                        any(Runnable.class)
                );
    }

    @Test
    void 두_작업이_모두_실패하면_false를_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("공유 받은 일정 및 공유 한 일정 삭제"),
                any(Runnable.class)
        )).thenAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);

            try {
                action.run();
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("일정 참여자 및 일정 삭제"),
                any(Runnable.class)
        )).thenAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);

            try {
                action.run();
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        doThrow(new RuntimeException("공유 일정 삭제 실패"))
                .when(scheduleShareService)
                .deleteAllShared(USER_ID);

        doThrow(new RuntimeException("일정 삭제 실패"))
                .when(scheduleService)
                .deleteAllSchedules(USER_ID);

        // when
        boolean result = scheduleCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        assertThat(result)
                .isFalse();

        verify(scheduleShareService)
                .deleteAllShared(USER_ID);

        verify(scheduleService)
                .deleteAllSchedules(USER_ID);

        verify(loggingExecutor, times(2))
                .execute(
                        eq(OUTBOX_ID),
                        anyString(),
                        any(Runnable.class)
                );
    }
}