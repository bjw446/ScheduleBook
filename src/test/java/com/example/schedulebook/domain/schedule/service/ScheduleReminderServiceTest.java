package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus;
import com.example.schedulebook.domain.schedule.event.ScheduleReminderEvent;
import com.example.schedulebook.domain.schedule.repository.ScheduleReminderRepository;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleReminderServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SCHEDULE_ID = 100L;
    private static final Long REMINDER_ID = 1000L;

    private static final LocalDate SCHEDULE_DATE =
            LocalDate.of(2026, 9, 16);

    private static final LocalTime START_TIME =
            LocalTime.of(10, 30);

    private static final LocalDateTime REMINDER_TIME =
            LocalDateTime.of(2026, 9, 16, 10, 30);

    private static final String CLAIM_TOKEN =
            "claim-token";

    @Mock
    private ScheduleReminderRepository scheduleReminderRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private Schedule schedule;

    @Mock
    private User user;

    @Mock
    private ScheduleReminder scheduleReminder;

    private ScheduleReminderService scheduleReminderService;

    @BeforeEach
    void setUp() {
        scheduleReminderService = new ScheduleReminderService(
                scheduleReminderRepository,
                outboxService
        );
    }

    @Test
    void 일정_생성시_알림_3개_저장() {
        // given
        when(schedule.getScheduleDate()).thenReturn(SCHEDULE_DATE);
        when(schedule.getStartTime()).thenReturn(START_TIME);

        // when
        scheduleReminderService.save(schedule);

        // then
        ArgumentCaptor<ScheduleReminder> captor =
                ArgumentCaptor.forClass(ScheduleReminder.class);

        verify(scheduleReminderRepository, times(3))
                .save(captor.capture());

        List<ScheduleReminder> reminders = captor.getAllValues();

        assertThat(reminders)
                .hasSize(3);

        assertThat(reminders.get(0).getSchedule())
                .isSameAs(schedule);

        assertThat(reminders.get(0).getReminderTime())
                .isEqualTo(REMINDER_TIME);

        assertThat(reminders.get(1).getSchedule())
                .isSameAs(schedule);

        assertThat(reminders.get(1).getReminderTime())
                .isEqualTo(REMINDER_TIME.minusMinutes(10));

        assertThat(reminders.get(2).getSchedule())
                .isSameAs(schedule);

        assertThat(reminders.get(2).getReminderTime())
                .isEqualTo(REMINDER_TIME.minusDays(1));

        verifyNoInteractions(outboxService);
    }

    @Test
    void 일정_변경시_기존_알림삭제_대기중_아웃박스취소_새로운_알림생성() {
        // given
        when(schedule.getId()).thenReturn(SCHEDULE_ID);
        when(schedule.getScheduleDate()).thenReturn(SCHEDULE_DATE);
        when(schedule.getStartTime()).thenReturn(START_TIME);

        // when
        scheduleReminderService.refresh(schedule);

        // then
        verify(scheduleReminderRepository)
                .deleteBySchedule_Id(SCHEDULE_ID);

        verify(outboxService)
                .cancelPending(
                        OutboxAggregateType.SCHEDULE,
                        String.valueOf(SCHEDULE_ID),
                        OutboxEventType.SCHEDULE_REMINDER
                );

        verify(scheduleReminderRepository, times(3))
                .save(any(ScheduleReminder.class));
    }

    @Test
    void 알림_처리시_클레임과_상태가_일치하면_아웃박스저장후_SENT_처리() {
        // given
        when(scheduleReminderRepository.findById(REMINDER_ID))
                .thenReturn(Optional.of(scheduleReminder));

        when(scheduleReminder.getClaimToken())
                .thenReturn(CLAIM_TOKEN);

        when(scheduleReminder.getScheduleReminderStatus())
                .thenReturn(ScheduleReminderStatus.PROCESSING);

        when(scheduleReminder.getSchedule())
                .thenReturn(schedule);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(schedule.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(USER_ID);

        when(schedule.getTitle())
                .thenReturn("테스트 일정");

        when(scheduleReminder.getReminderTime())
                .thenReturn(REMINDER_TIME);

        when(scheduleReminderRepository.markSent(
                REMINDER_ID,
                CLAIM_TOKEN
        )).thenReturn(1);

        // when
        boolean result = scheduleReminderService.processReminderSent(
                REMINDER_ID,
                CLAIM_TOKEN
        );

        // then
        assertThat(result)
                .isTrue();

        ArgumentCaptor<ScheduleReminderEvent> eventCaptor =
                ArgumentCaptor.forClass(ScheduleReminderEvent.class);

        verify(outboxService)
                .save(
                        anyString(),
                        eq(OutboxAggregateType.SCHEDULE),
                        eq(String.valueOf(SCHEDULE_ID)),
                        eq(OutboxEventType.SCHEDULE_REMINDER),
                        eventCaptor.capture()
                );

        ScheduleReminderEvent event =
                eventCaptor.getValue();

        assertThat(event.scheduleId())
                .isEqualTo(SCHEDULE_ID);

        assertThat(event.receiverId())
                .isEqualTo(USER_ID);

        assertThat(event.title())
                .isEqualTo("테스트 일정");

        assertThat(event.reminderTime())
                .isEqualTo(REMINDER_TIME);

        verify(scheduleReminderRepository)
                .markSent(REMINDER_ID, CLAIM_TOKEN);
    }

    @Test
    void 알림_처리시_클레임토큰이다르면_false() {
        // given
        when(scheduleReminderRepository.findById(REMINDER_ID))
                .thenReturn(Optional.of(scheduleReminder));

        when(scheduleReminder.getClaimToken())
                .thenReturn("different-token");

        // when
        boolean result = scheduleReminderService.processReminderSent(
                REMINDER_ID,
                CLAIM_TOKEN
        );

        // then
        assertThat(result)
                .isFalse();

        verify(scheduleReminderRepository, never())
                .markSent(anyLong(), anyString());

        verifyNoInteractions(outboxService);
    }

    @Test
    void 알림_처리시_PROCESSING상태가아니면_false() {
        // given
        when(scheduleReminderRepository.findById(REMINDER_ID))
                .thenReturn(Optional.of(scheduleReminder));

        when(scheduleReminder.getClaimToken())
                .thenReturn(CLAIM_TOKEN);

        when(scheduleReminder.getScheduleReminderStatus())
                .thenReturn(ScheduleReminderStatus.PENDING);

        // when
        boolean result = scheduleReminderService.processReminderSent(
                REMINDER_ID,
                CLAIM_TOKEN
        );

        // then
        assertThat(result)
                .isFalse();

        verify(scheduleReminderRepository, never())
                .markSent(anyLong(), anyString());

        verifyNoInteractions(outboxService);
    }

    @Test
    void 알림이_존재하지않으면_예외() {
        // given
        when(scheduleReminderRepository.findById(REMINDER_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                scheduleReminderService.processReminderSent(
                        REMINDER_ID,
                        CLAIM_TOKEN
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_REMINDER_NOT_FOUND);

        verifyNoInteractions(outboxService);
    }

    @Test
    void 알림_SENT_상태변경에_실패하면_예외() {
        // given
        when(scheduleReminderRepository.findById(REMINDER_ID))
                .thenReturn(Optional.of(scheduleReminder));

        when(scheduleReminder.getClaimToken())
                .thenReturn(CLAIM_TOKEN);

        when(scheduleReminder.getScheduleReminderStatus())
                .thenReturn(ScheduleReminderStatus.PROCESSING);

        when(scheduleReminder.getSchedule())
                .thenReturn(schedule);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(schedule.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(USER_ID);

        when(schedule.getTitle())
                .thenReturn("테스트 일정");

        when(scheduleReminder.getReminderTime())
                .thenReturn(REMINDER_TIME);

        when(scheduleReminderRepository.markSent(
                REMINDER_ID,
                CLAIM_TOKEN
        )).thenReturn(0);

        // when & then
        assertThatThrownBy(() ->
                scheduleReminderService.processReminderSent(
                        REMINDER_ID,
                        CLAIM_TOKEN
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_REMINDER_STATUS_CHANGE_FAILED);

        verify(outboxService)
                .save(
                        anyString(),
                        eq(OutboxAggregateType.SCHEDULE),
                        eq(String.valueOf(SCHEDULE_ID)),
                        eq(OutboxEventType.SCHEDULE_REMINDER),
                        any(ScheduleReminderEvent.class)
                );

        verify(scheduleReminderRepository)
                .markSent(REMINDER_ID, CLAIM_TOKEN);
    }

    @Test
    void 알림_PROCESSING_처리_성공() {
        // given
        LocalDateTime claimedAt =
                LocalDateTime.of(2026, 9, 16, 10, 0);

        when(scheduleReminderRepository.markProcessing(
                REMINDER_ID,
                CLAIM_TOKEN,
                claimedAt
        )).thenReturn(1);

        // when
        boolean result = scheduleReminderService.markProcessing(
                REMINDER_ID,
                CLAIM_TOKEN,
                claimedAt
        );

        // then
        assertThat(result)
                .isTrue();

        verify(scheduleReminderRepository)
                .markProcessing(
                        REMINDER_ID,
                        CLAIM_TOKEN,
                        claimedAt
                );
    }

    @Test
    void 알림_PROCESSING_처리_실패() {
        // given
        LocalDateTime claimedAt =
                LocalDateTime.of(2026, 9, 16, 10, 0);

        when(scheduleReminderRepository.markProcessing(
                REMINDER_ID,
                CLAIM_TOKEN,
                claimedAt
        )).thenReturn(0);

        // when
        boolean result = scheduleReminderService.markProcessing(
                REMINDER_ID,
                CLAIM_TOKEN,
                claimedAt
        );

        // then
        assertThat(result)
                .isFalse();

        verify(scheduleReminderRepository)
                .markProcessing(
                        REMINDER_ID,
                        CLAIM_TOKEN,
                        claimedAt
                );
    }

    @Test
    void 알림_SENT_처리_성공() {
        // given
        when(scheduleReminderRepository.markSent(
                REMINDER_ID,
                CLAIM_TOKEN
        )).thenReturn(1);

        // when
        boolean result = scheduleReminderService.markSent(
                REMINDER_ID,
                CLAIM_TOKEN
        );

        // then
        assertThat(result)
                .isTrue();

        verify(scheduleReminderRepository)
                .markSent(REMINDER_ID, CLAIM_TOKEN);
    }

    @Test
    void 알림_SENT_처리_실패() {
        // given
        when(scheduleReminderRepository.markSent(
                REMINDER_ID,
                CLAIM_TOKEN
        )).thenReturn(0);

        // when
        boolean result = scheduleReminderService.markSent(
                REMINDER_ID,
                CLAIM_TOKEN
        );

        // then
        assertThat(result)
                .isFalse();

        verify(scheduleReminderRepository)
                .markSent(REMINDER_ID, CLAIM_TOKEN);
    }

    @Test
    void 알림_PENDING_복구_성공() {
        // given
        when(scheduleReminderRepository.markPending(
                REMINDER_ID,
                CLAIM_TOKEN
        )).thenReturn(1);

        // when
        boolean result = scheduleReminderService.markPending(
                REMINDER_ID,
                CLAIM_TOKEN
        );

        // then
        assertThat(result)
                .isTrue();

        verify(scheduleReminderRepository)
                .markPending(REMINDER_ID, CLAIM_TOKEN);
    }

    @Test
    void 알림_PENDING_복구_실패() {
        // given
        when(scheduleReminderRepository.markPending(
                REMINDER_ID,
                CLAIM_TOKEN
        )).thenReturn(0);

        // when
        boolean result = scheduleReminderService.markPending(
                REMINDER_ID,
                CLAIM_TOKEN
        );

        // then
        assertThat(result)
                .isFalse();

        verify(scheduleReminderRepository)
                .markPending(REMINDER_ID, CLAIM_TOKEN);
    }

    @Test
    void 오래된_PROCESSING_알림_복구() {
        // given
        LocalDateTime threshold =
                LocalDateTime.of(2026, 9, 16, 9, 0);

        when(scheduleReminderRepository.recoverStuckReminders(threshold))
                .thenReturn(3);

        // when
        int result =
                scheduleReminderService.recoverStuckReminders(threshold);

        // then
        assertThat(result)
                .isEqualTo(3);

        verify(scheduleReminderRepository)
                .recoverStuckReminders(threshold);
    }

    @Test
    void 대기중인_알림_조회() {
        // given
        LocalDateTime now =
                LocalDateTime.of(2026, 9, 16, 10, 0);

        Pageable pageable =
                PageRequest.of(0, 20);

        List<ScheduleReminder> reminders =
                List.of(scheduleReminder);

        when(scheduleReminderRepository.findReminders(now, pageable))
                .thenReturn(reminders);

        // when
        List<ScheduleReminder> result =
                scheduleReminderService.findPendingReminders(
                        now,
                        pageable
                );

        // then
        assertThat(result)
                .isSameAs(reminders);

        verify(scheduleReminderRepository)
                .findReminders(now, pageable);
    }

    @Test
    void 대기중인_알림_개수_조회() {
        // given
        when(scheduleReminderRepository.countByScheduleReminderStatus(
                ScheduleReminderStatus.PENDING
        )).thenReturn(5L);

        // when
        long result =
                scheduleReminderService.countPendingReminders();

        // then
        assertThat(result)
                .isEqualTo(5L);

        verify(scheduleReminderRepository)
                .countByScheduleReminderStatus(
                        ScheduleReminderStatus.PENDING
                );
    }
}