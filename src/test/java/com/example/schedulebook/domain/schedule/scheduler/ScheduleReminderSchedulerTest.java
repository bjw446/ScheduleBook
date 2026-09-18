package com.example.schedulebook.domain.schedule.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.metrics.RetrySchedulerMetrics;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import com.example.schedulebook.domain.schedule.service.ScheduleReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ScheduleReminderSchedulerTest {

    private static final String METRIC = "schedule_reminder";
    private static final String RECOVERY_METRIC = "schedule_reminder_recovery";

    private static final Long REMINDER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;

    private static final LocalDateTime REMINDER_TIME =
            LocalDateTime.of(2026, 9, 18, 13, 0);

    @Mock
    private ScheduleReminderService scheduleReminderService;

    @Mock
    private RetrySchedulerMetrics retrySchedulerMetrics;

    @Mock
    private ScheduleReminder reminder;

    @Mock
    private Schedule schedule;

    @InjectMocks
    private ScheduleReminderScheduler scheduleReminderScheduler;

    @BeforeEach
    void setUp() {
        // 공통적으로 사용하는 reminder 자체의 ID만 설정한다.
        // schedule / reminderTime은 실제로 해당 값을 사용하는 테스트에서만 설정한다.
        lenient().when(reminder.getId()).thenReturn(REMINDER_ID);
    }

    @Test
    void Pending_알림이_없으면_조회_후_종료한다() {
        // given
        when(scheduleReminderService.findPendingReminders(
                any(LocalDateTime.class),
                eq(PageRequest.of(0, CommonConst.BATCH_SIZE))
        )).thenReturn(List.of());

        // when
        scheduleReminderScheduler.sendScheduleReminders();

        // then
        verify(retrySchedulerMetrics).schedulerRun(METRIC);

        verify(scheduleReminderService).findPendingReminders(
                any(LocalDateTime.class),
                eq(PageRequest.of(0, CommonConst.BATCH_SIZE))
        );

        verifyNoMoreInteractions(scheduleReminderService);
    }

    @Test
    void 알림을_조회하고_Processing_선점에_성공하면_알림을_처리한다() {
        // given
        givenReminderDetails();

        when(scheduleReminderService.findPendingReminders(
                any(LocalDateTime.class),
                eq(PageRequest.of(0, CommonConst.BATCH_SIZE))
        )).thenReturn(List.of(reminder), List.of());

        when(scheduleReminderService.markProcessing(
                eq(REMINDER_ID),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(true);

        when(scheduleReminderService.processReminderSent(
                eq(REMINDER_ID),
                anyString()
        )).thenReturn(true);

        // when
        scheduleReminderScheduler.sendScheduleReminders();

        // then
        verify(scheduleReminderService).markProcessing(
                eq(REMINDER_ID),
                anyString(),
                any(LocalDateTime.class)
        );

        verify(scheduleReminderService).processReminderSent(
                eq(REMINDER_ID),
                anyString()
        );

        verify(retrySchedulerMetrics).processed(METRIC);
        verify(retrySchedulerMetrics).success(METRIC);
    }

    @Test
    void Processing_선점에_실패하면_해당_알림은_처리하지_않는다() {
        // given
        when(scheduleReminderService.findPendingReminders(
                any(LocalDateTime.class),
                eq(PageRequest.of(0, CommonConst.BATCH_SIZE))
        )).thenReturn(
                List.of(reminder),
                List.of()
        );

        when(scheduleReminderService.markProcessing(
                eq(REMINDER_ID),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(false);

        // when
        scheduleReminderScheduler.sendScheduleReminders();

        // then
        verify(scheduleReminderService).markProcessing(
                eq(REMINDER_ID),
                anyString(),
                any(LocalDateTime.class)
        );

        verify(scheduleReminderService, never())
                .processReminderSent(anyLong(), anyString());

        verify(retrySchedulerMetrics, never()).processed(METRIC);
        verify(retrySchedulerMetrics, never()).success(METRIC);
    }

    @Test
    void 알림_처리_결과가_false면_추가_복구를_수행하지_않는다() {
        // given
        givenReminderDetails();

        when(scheduleReminderService.findPendingReminders(
                any(LocalDateTime.class),
                eq(PageRequest.of(0, CommonConst.BATCH_SIZE))
        )).thenReturn(
                List.of(reminder),
                List.of()
        );

        when(scheduleReminderService.markProcessing(
                eq(REMINDER_ID),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(true);

        when(scheduleReminderService.processReminderSent(
                eq(REMINDER_ID),
                anyString()
        )).thenReturn(false);

        // when
        scheduleReminderScheduler.sendScheduleReminders();

        // then
        verify(scheduleReminderService).processReminderSent(
                eq(REMINDER_ID),
                anyString()
        );

        verify(scheduleReminderService, never())
                .markPending(anyLong(), anyString());

        verify(retrySchedulerMetrics).processed(METRIC);
        verify(retrySchedulerMetrics, never()).success(METRIC);
        verify(retrySchedulerMetrics, never()).error(METRIC);
    }

    @Test
    void 알림_처리중_예외가_발생하면_Pending으로_복구한다() {
        // given
        givenReminderDetails();

        when(scheduleReminderService.findPendingReminders(
                any(LocalDateTime.class),
                eq(PageRequest.of(0, CommonConst.BATCH_SIZE))
        )).thenReturn(
                List.of(reminder),
                List.of()
        );

        when(scheduleReminderService.markProcessing(
                eq(REMINDER_ID),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(true);

        when(scheduleReminderService.processReminderSent(
                eq(REMINDER_ID),
                anyString()
        )).thenThrow(new RuntimeException("알림 처리 실패"));

        when(scheduleReminderService.markPending(
                eq(REMINDER_ID),
                anyString()
        )).thenReturn(true);

        // when
        scheduleReminderScheduler.sendScheduleReminders();

        // then
        verify(scheduleReminderService).processReminderSent(
                eq(REMINDER_ID),
                anyString()
        );

        verify(retrySchedulerMetrics).processed(METRIC);
        verify(retrySchedulerMetrics).error(METRIC);

        verify(scheduleReminderService).markPending(
                eq(REMINDER_ID),
                anyString()
        );
    }

    @Test
    void Pending_복구에_실패하면_Error_Metric을_추가로_기록한다() {
        // given
        givenReminderDetails();

        when(scheduleReminderService.findPendingReminders(
                any(LocalDateTime.class),
                eq(PageRequest.of(0, CommonConst.BATCH_SIZE))
        )).thenReturn(
                List.of(reminder),
                List.of()
        );

        when(scheduleReminderService.markProcessing(
                eq(REMINDER_ID),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(true);

        when(scheduleReminderService.processReminderSent(
                eq(REMINDER_ID),
                anyString()
        )).thenThrow(new RuntimeException("알림 처리 실패"));

        when(scheduleReminderService.markPending(
                eq(REMINDER_ID),
                anyString()
        )).thenReturn(false);

        // when
        scheduleReminderScheduler.sendScheduleReminders();

        // then
        verify(scheduleReminderService).markPending(
                eq(REMINDER_ID),
                anyString()
        );

        // 처리 실패 1회 + Pending 복구 실패 1회
        verify(retrySchedulerMetrics, times(2)).error(METRIC);
    }

    @Test
    void 여러_배치가_존재하면_최대_배치_횟수만큼_반복한다() {
        // given
        when(scheduleReminderService.findPendingReminders(
                any(LocalDateTime.class),
                eq(PageRequest.of(0, CommonConst.BATCH_SIZE))
        )).thenReturn(List.of(reminder));

        when(scheduleReminderService.markProcessing(
                eq(REMINDER_ID),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(false);

        // when
        scheduleReminderScheduler.sendScheduleReminders();

        // then
        verify(scheduleReminderService, times(CommonConst.MAX_BATCHES_PER_RUN))
                .findPendingReminders(
                        any(LocalDateTime.class),
                        eq(PageRequest.of(0, CommonConst.BATCH_SIZE))
                );
    }

    @Test
    void Stuck_Reminder를_복구한다() {
        // given
        LocalDateTime threshold = LocalDateTime.now()
                .minusMinutes(
                        CommonConst.SCHEDULE_REMINDER_PROCESSING_TIMEOUT_MINUTES
                );

        when(scheduleReminderService.recoverStuckReminders(
                any(LocalDateTime.class)
        )).thenReturn(3);

        // when
        scheduleReminderScheduler.recoverStuckReminders();

        // then
        verify(retrySchedulerMetrics).schedulerRun(RECOVERY_METRIC);

        verify(scheduleReminderService).recoverStuckReminders(
                any(LocalDateTime.class)
        );

        verify(retrySchedulerMetrics).recovered(
                RECOVERY_METRIC,
                3
        );
    }

    @Test
    void 복구할_Stuck_Reminder가_없으면_Recovered_Metric을_기록하지_않는다() {
        // given
        when(scheduleReminderService.recoverStuckReminders(
                any(LocalDateTime.class)
        )).thenReturn(0);

        // when
        scheduleReminderScheduler.recoverStuckReminders();

        // then
        verify(retrySchedulerMetrics).schedulerRun(RECOVERY_METRIC);

        verify(scheduleReminderService).recoverStuckReminders(
                any(LocalDateTime.class)
        );

        verify(retrySchedulerMetrics, never())
                .recovered(anyString(), anyInt());
    }

    @Test
    void Pending_Gauge를_등록한다() {
        // given
        ArgumentCaptor<Supplier<Number>> supplierCaptor =
                ArgumentCaptor.forClass(Supplier.class);

        // when
        scheduleReminderScheduler.registerMetrics();

        // then
        verify(retrySchedulerMetrics).registerPendingGauge(
                eq(METRIC),
                supplierCaptor.capture()
        );

        when(scheduleReminderService.countPendingReminders())
                .thenReturn(5L);

        assertThat(supplierCaptor.getValue().get())
                .isEqualTo(5L);

        verify(scheduleReminderService)
                .countPendingReminders();
    }

    private void givenReminderDetails() {
        when(reminder.getId()).thenReturn(REMINDER_ID);
        when(reminder.getSchedule()).thenReturn(schedule);
        when(schedule.getId()).thenReturn(SCHEDULE_ID);
        when(reminder.getReminderTime()).thenReturn(REMINDER_TIME);
    }
}