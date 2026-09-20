package com.example.schedulebook.domain.scheduleshare.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.scheduleshare.event.ScheduleSharedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleSharedProcessorTest {

    private static final Long OUTBOX_ID = 100L;
    private static final Long RECEIVER_ID = 1L;
    private static final String OWNER_NICKNAME = "owner";
    private static final Long SHARE_ID = 10L;
    private static final String EVENT_ID = "event-id";

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRetryService notificationRetryService;

    @Mock
    private ScheduleSharedEvent event;

    private ScheduleSharedProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ScheduleSharedProcessor(
                notificationService,
                notificationRetryService
        );
    }

    @Test
    void 지원하는_이벤트_타입을_반환한다() {
        // when
        Class<ScheduleSharedEvent> result = processor.supports();

        // then
        assertThat(result).isEqualTo(ScheduleSharedEvent.class);
    }

    @Test
    void 일정_공유_이벤트를_정상_처리하면_일정_공유_알림을_생성한다() {
        // given
        when(event.receiverId()).thenReturn(RECEIVER_ID);
        when(event.ownerNickname()).thenReturn(OWNER_NICKNAME);
        when(event.shareId()).thenReturn(SHARE_ID);

        // when
        processor.process(OUTBOX_ID, event);

        // then
        verify(notificationService)
                .createScheduleSharedNotification(
                        RECEIVER_ID,
                        OWNER_NICKNAME,
                        SHARE_ID
                );

        verifyNoInteractions(notificationRetryService);
    }

    @Test
    void 일정_공유_알림_생성에_실패하면_Retry_저장을_수행한다() {
        // given
        RuntimeException exception = new RuntimeException("notification create failed");

        when(event.receiverId()).thenReturn(RECEIVER_ID);
        when(event.ownerNickname()).thenReturn(OWNER_NICKNAME);
        when(event.shareId()).thenReturn(SHARE_ID);
        when(event.eventId()).thenReturn(EVENT_ID);

        doThrow(exception)
                .when(notificationService)
                .createScheduleSharedNotification(
                        RECEIVER_ID,
                        OWNER_NICKNAME,
                        SHARE_ID
                );

        // when
        processor.process(OUTBOX_ID, event);

        // then
        verify(notificationService)
                .createScheduleSharedNotification(
                        RECEIVER_ID,
                        OWNER_NICKNAME,
                        SHARE_ID
                );

        verify(notificationRetryService)
                .save(
                        EVENT_ID,
                        OUTBOX_ID,
                        RECEIVER_ID,
                        NotificationType.SCHEDULE_SHARED,
                        event,
                        exception.getMessage()
                );
    }

    @Test
    void Retry_저장에_실패하면_알림_Retry_저장_실패_예외를_발생시킨다() {
        // given
        RuntimeException notificationException =
                new RuntimeException("notification create failed");

        RuntimeException retryException =
                new RuntimeException("retry save failed");

        when(event.receiverId()).thenReturn(RECEIVER_ID);
        when(event.ownerNickname()).thenReturn(OWNER_NICKNAME);
        when(event.shareId()).thenReturn(SHARE_ID);
        when(event.eventId()).thenReturn(EVENT_ID);

        doThrow(notificationException)
                .when(notificationService)
                .createScheduleSharedNotification(
                        RECEIVER_ID,
                        OWNER_NICKNAME,
                        SHARE_ID
                );

        doThrow(retryException)
                .when(notificationRetryService)
                .save(
                        EVENT_ID,
                        OUTBOX_ID,
                        RECEIVER_ID,
                        NotificationType.SCHEDULE_SHARED,
                        event,
                        notificationException.getMessage()
                );

        // when & then
        assertThatThrownBy(() ->
                processor.process(OUTBOX_ID, event)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_SAVE_FAILED);

        verify(notificationService)
                .createScheduleSharedNotification(
                        RECEIVER_ID,
                        OWNER_NICKNAME,
                        SHARE_ID
                );

        verify(notificationRetryService)
                .save(
                        EVENT_ID,
                        OUTBOX_ID,
                        RECEIVER_ID,
                        NotificationType.SCHEDULE_SHARED,
                        event,
                        notificationException.getMessage()
                );
    }
}