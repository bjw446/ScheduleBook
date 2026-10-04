package com.example.schedulebook.domain.comment.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notificationretry.service.ProcessedNotificationRetryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentRetryProcessorTest {

    private static final String EVENT_ID = "event-1";
    private static final Long RECEIVER_ID = 2L;
    private static final Long SCHEDULE_ID = 10L;
    private static final Long WRITER_ID = 1L;
    private static final String WRITER_NICKNAME = "작성자";
    private static final String PAYLOAD = "payload";

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProcessedNotificationRetryService processedNotificationRetryService;

    @Mock
    private NotificationRetry notificationRetry;

    @InjectMocks
    private CommentRetryProcessor commentRetryProcessor;

    @Test
    void process_이미_처리된_재시도_알림이면_처리하지_않고_종료한다() {
        // given
        when(processedNotificationRetryService.prepareProcessedNotificationRetry(notificationRetry))
                .thenReturn(true);

        // when
        commentRetryProcessor.process(notificationRetry);

        // then
        verify(processedNotificationRetryService)
                .prepareProcessedNotificationRetry(notificationRetry);

        verifyNoInteractions(
                objectMapper,
                notificationService
        );
    }

    @Test
    void process_일정_댓글_알림이면_일정_댓글_알림을_재생성한다() throws Exception {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                null
        );

        when(processedNotificationRetryService.prepareProcessedNotificationRetry(notificationRetry))
                .thenReturn(false);

        when(notificationRetry.getPayload())
                .thenReturn(PAYLOAD);

        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.SCHEDULE_COMMENT);

        when(notificationRetry.getReceiverId())
                .thenReturn(RECEIVER_ID);

        when(objectMapper.readValue(PAYLOAD, CommentCreatedEvent.class))
                .thenReturn(event);

        // when
        commentRetryProcessor.process(notificationRetry);

        // then
        verify(notificationService)
                .createScheduleCommentNotification(
                        RECEIVER_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );
    }

    @Test
    void process_대댓글_알림이면_대댓글_알림을_재생성한다() throws Exception {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                20L
        );

        when(processedNotificationRetryService.prepareProcessedNotificationRetry(notificationRetry))
                .thenReturn(false);

        when(notificationRetry.getPayload())
                .thenReturn(PAYLOAD);

        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.COMMENT_REPLY);

        when(notificationRetry.getReceiverId())
                .thenReturn(RECEIVER_ID);

        when(objectMapper.readValue(PAYLOAD, CommentCreatedEvent.class))
                .thenReturn(event);

        // when
        commentRetryProcessor.process(notificationRetry);

        // then
        verify(notificationService)
                .createCommentReplyNotification(
                        RECEIVER_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );
    }

    @Test
    void process_지원하지_않는_알림_타입이면_예외가_발생한다() throws Exception {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                null
        );

        when(processedNotificationRetryService.prepareProcessedNotificationRetry(notificationRetry))
                .thenReturn(false);

        when(notificationRetry.getPayload())
                .thenReturn(PAYLOAD);

        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.FRIEND_REQUEST);

        when(objectMapper.readValue(PAYLOAD, CommentCreatedEvent.class))
                .thenReturn(event);

        // when & then
        assertThatThrownBy(() ->
                commentRetryProcessor.process(notificationRetry)
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.INVALID_NOTIFICATION_TYPE)
                );

        verifyNoInteractions(notificationService);
    }

    @Test
    void process_재시도_payload_역직렬화에_실패하면_예외가_발생한다() throws Exception {
        // given
        when(processedNotificationRetryService.prepareProcessedNotificationRetry(notificationRetry))
                .thenReturn(false);

        when(notificationRetry.getPayload())
                .thenReturn(PAYLOAD);

        when(objectMapper.readValue(PAYLOAD, CommentCreatedEvent.class))
                .thenThrow(new JsonProcessingException("역직렬화 실패") {
                });

        // when & then
        assertThatThrownBy(() ->
                commentRetryProcessor.process(notificationRetry)
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.JSON_DESERIALIZATION_FAILED)
                );

        verifyNoInteractions(notificationService);
    }
}