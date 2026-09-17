package com.example.schedulebook.domain.schedule.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.schedule.event.ScheduleReminderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleReminderProcessorTest {

    private static final Long OUTBOX_ID = 100L;
    private static final Long RECEIVER_ID = 1L;
    private static final Long SCHEDULE_ID = 200L;
    private static final String EVENT_ID = "event-uuid";
    private static final String TITLE = "테스트 일정";

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRetryService notificationRetryService;

    @Mock
    private ScheduleReminderEvent event;

    private ScheduleReminderProcessor scheduleReminderProcessor;

    @BeforeEach
    void setUp() {
        scheduleReminderProcessor = new ScheduleReminderProcessor(
                notificationService,
                notificationRetryService
        );
    }

    @Test
    void 지원하는_이벤트_타입을_반환한다() {
        // when
        Class<ScheduleReminderEvent> result =
                scheduleReminderProcessor.supports();

        // then
        assertThat(result)
                .isEqualTo(ScheduleReminderEvent.class);
    }

    @Test
    void 일정_알림_생성에_성공하면_Retry를_저장하지_않는다() {
        // given
        when(event.receiverId())
                .thenReturn(RECEIVER_ID);

        when(event.scheduleId())
                .thenReturn(SCHEDULE_ID);

        when(event.title())
                .thenReturn(TITLE);

        // when
        scheduleReminderProcessor.process(
                OUTBOX_ID,
                event
        );

        // then
        verify(notificationService)
                .createScheduleReminderNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        TITLE
                );

        verifyNoInteractions(notificationRetryService);
    }

    @Test
    void 일정_알림_생성에_실패하면_Retry를_저장한다() {
        // given
        RuntimeException exception =
                new RuntimeException("알림 생성 실패");

        when(event.eventId())
                .thenReturn(EVENT_ID);

        when(event.receiverId())
                .thenReturn(RECEIVER_ID);

        when(event.scheduleId())
                .thenReturn(SCHEDULE_ID);

        when(event.title())
                .thenReturn(TITLE);

        doThrow(exception)
                .when(notificationService)
                .createScheduleReminderNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        TITLE
                );

        // when
        scheduleReminderProcessor.process(
                OUTBOX_ID,
                event
        );

        // then
        verify(notificationService)
                .createScheduleReminderNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        TITLE
                );

        verify(notificationRetryService)
                .save(
                        eq(EVENT_ID),
                        eq(OUTBOX_ID),
                        eq(RECEIVER_ID),
                        eq(NotificationType.SCHEDULE_REMINDER),
                        eq(event),
                        eq("알림 생성 실패")
                );
    }

    @Test
    void 일정_알림_생성_실패시_Retry에_원본_이벤트와_예외메시지를_저장한다() {
        // given
        RuntimeException exception =
                new RuntimeException("알림 서버 일시 장애");

        when(event.eventId())
                .thenReturn(EVENT_ID);

        when(event.receiverId())
                .thenReturn(RECEIVER_ID);

        when(event.scheduleId())
                .thenReturn(SCHEDULE_ID);

        when(event.title())
                .thenReturn(TITLE);

        doThrow(exception)
                .when(notificationService)
                .createScheduleReminderNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        TITLE
                );

        // when
        scheduleReminderProcessor.process(
                OUTBOX_ID,
                event
        );

        // then
        ArgumentCaptor<ScheduleReminderEvent> eventCaptor =
                ArgumentCaptor.forClass(ScheduleReminderEvent.class);

        verify(notificationRetryService)
                .save(
                        eq(EVENT_ID),
                        eq(OUTBOX_ID),
                        eq(RECEIVER_ID),
                        eq(NotificationType.SCHEDULE_REMINDER),
                        eventCaptor.capture(),
                        eq("알림 서버 일시 장애")
                );

        assertThat(eventCaptor.getValue())
                .isSameAs(event);
    }

    @Test
    void Retry_저장에_실패하면_예외를_던진다() {
        // given
        RuntimeException notificationException =
                new RuntimeException("알림 생성 실패");

        RuntimeException retryException =
                new RuntimeException("Retry 저장 실패");

        when(event.eventId())
                .thenReturn(EVENT_ID);

        when(event.receiverId())
                .thenReturn(RECEIVER_ID);

        when(event.scheduleId())
                .thenReturn(SCHEDULE_ID);

        when(event.title())
                .thenReturn(TITLE);

        doThrow(notificationException)
                .when(notificationService)
                .createScheduleReminderNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        TITLE
                );

        doThrow(retryException)
                .when(notificationRetryService)
                .save(
                        eq(EVENT_ID),
                        eq(OUTBOX_ID),
                        eq(RECEIVER_ID),
                        eq(NotificationType.SCHEDULE_REMINDER),
                        eq(event),
                        eq("알림 생성 실패")
                );

        // when & then
        assertThatThrownBy(() ->
                scheduleReminderProcessor.process(
                        OUTBOX_ID,
                        event
                )
        )
                .isInstanceOf(BaseException.class)
                .hasCause(retryException)
                .extracting(e ->
                        ((BaseException) e).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_SAVE_FAILED);

        verify(notificationService)
                .createScheduleReminderNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        TITLE
                );

        verify(notificationRetryService)
                .save(
                        eq(EVENT_ID),
                        eq(OUTBOX_ID),
                        eq(RECEIVER_ID),
                        eq(NotificationType.SCHEDULE_REMINDER),
                        eq(event),
                        eq("알림 생성 실패")
                );
    }

    @Test
    void 알림_생성_실패시_Retry_저장만_수행하고_알림을_다시_생성하지_않는다() {
        // given
        RuntimeException exception =
                new RuntimeException("알림 생성 실패");

        when(event.eventId())
                .thenReturn(EVENT_ID);

        when(event.receiverId())
                .thenReturn(RECEIVER_ID);

        when(event.scheduleId())
                .thenReturn(SCHEDULE_ID);

        when(event.title())
                .thenReturn(TITLE);

        doThrow(exception)
                .when(notificationService)
                .createScheduleReminderNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        TITLE
                );

        // when
        scheduleReminderProcessor.process(
                OUTBOX_ID,
                event
        );

        // then
        verify(notificationService, times(1))
                .createScheduleReminderNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        TITLE
                );

        verify(notificationRetryService, times(1))
                .save(
                        eq(EVENT_ID),
                        eq(OUTBOX_ID),
                        eq(RECEIVER_ID),
                        eq(NotificationType.SCHEDULE_REMINDER),
                        eq(event),
                        eq("알림 생성 실패")
                );
    }
}