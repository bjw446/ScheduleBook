package com.example.schedulebook.domain.notificationretry.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.processor.CommentRetryProcessor;
import com.example.schedulebook.domain.friend.processor.FriendRetryProcessor;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.schedule.processor.ScheduleRetryProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRetryProcessorTest {

    private static final Long NOTIFICATION_RETRY_ID = 1L;

    @Mock
    private CommentRetryProcessor commentRetryProcessor;

    @Mock
    private FriendRetryProcessor friendRetryProcessor;

    @Mock
    private ScheduleRetryProcessor scheduleRetryProcessor;

    @Mock
    private NotificationRetryService notificationRetryService;

    @Mock
    private NotificationRetry notificationRetry;

    @InjectMocks
    private NotificationRetryProcessor notificationRetryProcessor;

    @Test
    void dispatch_COMMENT_REPLY_유형이면_CommentRetryProcessor를_실행한다() {
        // given
        givenNotificationRetry(NotificationType.COMMENT_REPLY);

        // when
        notificationRetryProcessor.dispatch(NOTIFICATION_RETRY_ID);

        // then
        verify(notificationRetryService).findById(NOTIFICATION_RETRY_ID);
        verify(commentRetryProcessor).process(notificationRetry);

        verifyNoInteractions(friendRetryProcessor, scheduleRetryProcessor);
    }

    @Test
    void dispatch_SCHEDULE_COMMENT_유형이면_CommentRetryProcessor를_실행한다() {
        // given
        givenNotificationRetry(NotificationType.SCHEDULE_COMMENT);

        // when
        notificationRetryProcessor.dispatch(NOTIFICATION_RETRY_ID);

        // then
        verify(commentRetryProcessor).process(notificationRetry);

        verifyNoInteractions(friendRetryProcessor, scheduleRetryProcessor);
    }

    @Test
    void dispatch_FRIEND_REQUEST_유형이면_FriendRetryProcessor를_실행한다() {
        // given
        givenNotificationRetry(NotificationType.FRIEND_REQUEST);

        // when
        notificationRetryProcessor.dispatch(NOTIFICATION_RETRY_ID);

        // then
        verify(friendRetryProcessor).process(notificationRetry);

        verifyNoInteractions(commentRetryProcessor, scheduleRetryProcessor);
    }

    @Test
    void dispatch_FRIEND_ACCEPTED_유형이면_FriendRetryProcessor를_실행한다() {
        // given
        givenNotificationRetry(NotificationType.FRIEND_ACCEPTED);

        // when
        notificationRetryProcessor.dispatch(NOTIFICATION_RETRY_ID);

        // then
        verify(friendRetryProcessor).process(notificationRetry);

        verifyNoInteractions(commentRetryProcessor, scheduleRetryProcessor);
    }

    @Test
    void dispatch_SCHEDULE_SHARED_유형이면_ScheduleRetryProcessor를_실행한다() {
        // given
        givenNotificationRetry(NotificationType.SCHEDULE_SHARED);

        // when
        notificationRetryProcessor.dispatch(NOTIFICATION_RETRY_ID);

        // then
        verify(scheduleRetryProcessor).process(notificationRetry);

        verifyNoInteractions(commentRetryProcessor, friendRetryProcessor);
    }

    @Test
    void dispatch_SCHEDULE_REMINDER_유형이면_ScheduleRetryProcessor를_실행한다() {
        // given
        givenNotificationRetry(NotificationType.SCHEDULE_REMINDER);

        // when
        notificationRetryProcessor.dispatch(NOTIFICATION_RETRY_ID);

        // then
        verify(scheduleRetryProcessor).process(notificationRetry);

        verifyNoInteractions(commentRetryProcessor, friendRetryProcessor);
    }

    @Test
    void dispatch_지원하지_않는_알림_유형이면_INVALID_NOTIFICATION_TYPE_예외를_발생시킨다() {
        // given
        givenNotificationRetry(NotificationType.REFRESH_REPLAY_USER);

        // when & then
        assertThatThrownBy(() ->
                notificationRetryProcessor.dispatch(NOTIFICATION_RETRY_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.INVALID_NOTIFICATION_TYPE);

        verifyNoInteractions(
                commentRetryProcessor,
                friendRetryProcessor,
                scheduleRetryProcessor
        );
    }

    @Test
    void dispatch_재시도_대상_조회에_실패하면_예외를_전달한다() {
        // given
        RuntimeException exception =
                new RuntimeException("재시도 대상 조회 실패");

        when(notificationRetryService.findById(NOTIFICATION_RETRY_ID))
                .thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                notificationRetryProcessor.dispatch(NOTIFICATION_RETRY_ID)
        )
                .isSameAs(exception);

        verify(notificationRetryService).findById(NOTIFICATION_RETRY_ID);

        verifyNoInteractions(
                commentRetryProcessor,
                friendRetryProcessor,
                scheduleRetryProcessor
        );
    }

    private void givenNotificationRetry(NotificationType notificationType) {
        when(notificationRetryService.findById(NOTIFICATION_RETRY_ID))
                .thenReturn(notificationRetry);

        when(notificationRetry.getNotificationType())
                .thenReturn(notificationType);
    }
}