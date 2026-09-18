package com.example.schedulebook.domain.schedule.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notificationretry.service.ProcessedNotificationRetryService;
import com.example.schedulebook.domain.schedule.event.ScheduleReminderEvent;
import com.example.schedulebook.domain.scheduleshare.event.ScheduleSharedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleRetryProcessorTest {

    private static final Long RECEIVER_ID = 1L;
    private static final Long SCHEDULE_ID = 100L;
    private static final Long SHARE_ID = 200L;

    private static final String OWNER_NICKNAME = "홍길동";
    private static final String TITLE = "팀 회의";
    private static final String PAYLOAD = "{\"eventId\":\"event-uuid\"}";

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProcessedNotificationRetryService processedNotificationRetryService;

    @Mock
    private NotificationRetry notificationRetry;

    @Mock
    private ScheduleSharedEvent scheduleSharedEvent;

    @Mock
    private ScheduleReminderEvent scheduleReminderEvent;

    private ScheduleRetryProcessor scheduleRetryProcessor;

    @BeforeEach
    void setUp() {
        scheduleRetryProcessor = new ScheduleRetryProcessor(
                objectMapper,
                notificationService,
                processedNotificationRetryService
        );
    }

    @Test
    void 이미_처리된_Retry면_알림을_재생성하지_않고_종료한다() {
        // given
        when(processedNotificationRetryService
                .prepareProcessedNotificationRetry(notificationRetry))
                .thenReturn(true);

        // when
        scheduleRetryProcessor.process(notificationRetry);

        // then
        verifyNoInteractions(objectMapper);
        verifyNoInteractions(notificationService);
    }

    @Test
    void SCHEDULE_SHARED이면_공유_알림을_생성한다() throws Exception {
        // given
        when(processedNotificationRetryService
                .prepareProcessedNotificationRetry(notificationRetry))
                .thenReturn(false);

        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.SCHEDULE_SHARED);

        when(notificationRetry.getReceiverId())
                .thenReturn(RECEIVER_ID);

        when(notificationRetry.getPayload())
                .thenReturn(PAYLOAD);

        when(scheduleSharedEvent.ownerNickname())
                .thenReturn(OWNER_NICKNAME);

        when(scheduleSharedEvent.shareId())
                .thenReturn(SHARE_ID);

        when(objectMapper.readValue(
                PAYLOAD,
                ScheduleSharedEvent.class
        )).thenReturn(scheduleSharedEvent);

        // when
        scheduleRetryProcessor.process(notificationRetry);

        // then
        verify(objectMapper)
                .readValue(
                        PAYLOAD,
                        ScheduleSharedEvent.class
                );

        verify(notificationService)
                .createScheduleSharedNotification(
                        RECEIVER_ID,
                        OWNER_NICKNAME,
                        SHARE_ID
                );
    }

    @Test
    void SCHEDULE_REMINDER이면_일정_알림을_생성한다() throws Exception {
        // given
        when(processedNotificationRetryService
                .prepareProcessedNotificationRetry(notificationRetry))
                .thenReturn(false);

        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.SCHEDULE_REMINDER);

        when(notificationRetry.getReceiverId())
                .thenReturn(RECEIVER_ID);

        when(notificationRetry.getPayload())
                .thenReturn(PAYLOAD);

        when(scheduleReminderEvent.scheduleId())
                .thenReturn(SCHEDULE_ID);

        when(scheduleReminderEvent.title())
                .thenReturn(TITLE);

        when(objectMapper.readValue(
                PAYLOAD,
                ScheduleReminderEvent.class
        )).thenReturn(scheduleReminderEvent);

        // when
        scheduleRetryProcessor.process(notificationRetry);

        // then
        verify(objectMapper)
                .readValue(
                        PAYLOAD,
                        ScheduleReminderEvent.class
                );

        verify(notificationService)
                .createScheduleReminderNotification(
                        RECEIVER_ID,
                        SCHEDULE_ID,
                        TITLE
                );
    }

    @Test
    void 지원하지_않는_알림_타입이면_INVALID_NOTIFICATION_TYPE_예외를_던진다() {
        // given
        when(processedNotificationRetryService
                .prepareProcessedNotificationRetry(notificationRetry))
                .thenReturn(false);

        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.FRIEND_REQUEST);

        // when & then
        assertThatThrownBy(() ->
                scheduleRetryProcessor.process(notificationRetry)
        )
                .isInstanceOf(BaseException.class)
                .extracting(e ->
                        ((BaseException) e).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_NOTIFICATION_TYPE);

        verifyNoInteractions(objectMapper);
        verifyNoInteractions(notificationService);
    }

    @Test
    void JSON_역직렬화에_실패하면_JSON_DESERIALIZATION_FAILED_예외를_던진다()
            throws Exception {
        // given
        when(processedNotificationRetryService
                .prepareProcessedNotificationRetry(notificationRetry))
                .thenReturn(false);

        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.SCHEDULE_SHARED);

        when(notificationRetry.getPayload())
                .thenReturn(PAYLOAD);

        JsonProcessingException exception =
                mock(JsonProcessingException.class);

        when(objectMapper.readValue(
                PAYLOAD,
                ScheduleSharedEvent.class
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                scheduleRetryProcessor.process(notificationRetry)
        )
                .isInstanceOf(BaseException.class)
                .extracting(e ->
                        ((BaseException) e).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.JSON_DESERIALIZATION_FAILED);

        verify(objectMapper)
                .readValue(
                        PAYLOAD,
                        ScheduleSharedEvent.class
                );

        verifyNoInteractions(notificationService);
    }
}