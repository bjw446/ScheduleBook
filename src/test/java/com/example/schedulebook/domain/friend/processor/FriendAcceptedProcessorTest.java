package com.example.schedulebook.domain.friend.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendAcceptedProcessorTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRetryService notificationRetryService;

    private FriendAcceptedProcessor processor;

    private static final Long OUTBOX_ID = 100L;
    private static final Long REQUESTER_ID = 1L;
    private static final Long FRIEND_ID = 10L;

    private static final String EVENT_ID = "friend-accepted-event-1";
    private static final String ACCEPTER_NICKNAME = "accepter";

    private FriendAcceptedEvent event;

    @BeforeEach
    void setUp() {
        processor = new FriendAcceptedProcessor(
                notificationService,
                notificationRetryService
        );

        event = new FriendAcceptedEvent(
                EVENT_ID,
                REQUESTER_ID,
                ACCEPTER_NICKNAME,
                FRIEND_ID
        );
    }

    @Test
    void 지원하는_이벤트_타입으로_FriendAcceptedEvent를_반환한다() {
        // when
        Class<FriendAcceptedEvent> result = processor.supports();

        // then
        assertThat(result).isEqualTo(FriendAcceptedEvent.class);
    }

    @Test
    void 친구_수락_알림을_정상적으로_생성한다() {
        // when
        processor.process(OUTBOX_ID, event);

        // then
        verify(notificationService).createFriendAcceptedNotification(
                REQUESTER_ID,
                ACCEPTER_NICKNAME,
                FRIEND_ID
        );

        verifyNoInteractions(notificationRetryService);
    }

    @Test
    void 친구_수락_알림_생성에_실패하면_retry를_저장한다() {
        // given
        String errorMessage = "notification create failed";

        doThrow(new RuntimeException(errorMessage))
                .when(notificationService)
                .createFriendAcceptedNotification(
                        REQUESTER_ID,
                        ACCEPTER_NICKNAME,
                        FRIEND_ID
                );

        // when
        processor.process(OUTBOX_ID, event);

        // then
        verify(notificationService).createFriendAcceptedNotification(
                REQUESTER_ID,
                ACCEPTER_NICKNAME,
                FRIEND_ID
        );

        verify(notificationRetryService).save(
                eq(EVENT_ID),
                eq(OUTBOX_ID),
                eq(REQUESTER_ID),
                eq(NotificationType.FRIEND_ACCEPTED),
                same(event),
                eq(errorMessage)
        );
    }

    @Test
    void 친구_수락_알림_생성_실패와_retry_저장_실패가_동시에_발생하면_예외가_발생한다() {
        // given
        String notificationErrorMessage = "notification create failed";
        RuntimeException notificationException =
                new RuntimeException(notificationErrorMessage);

        doThrow(notificationException)
                .when(notificationService)
                .createFriendAcceptedNotification(
                        REQUESTER_ID,
                        ACCEPTER_NICKNAME,
                        FRIEND_ID
                );

        RuntimeException retryException =
                new RuntimeException("retry save failed");

        doThrow(retryException)
                .when(notificationRetryService)
                .save(
                        eq(EVENT_ID),
                        eq(OUTBOX_ID),
                        eq(REQUESTER_ID),
                        eq(NotificationType.FRIEND_ACCEPTED),
                        same(event),
                        eq(notificationErrorMessage)
                );

        // when & then
        assertThatThrownBy(() ->
                processor.process(OUTBOX_ID, event)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_SAVE_FAILED);

        verify(notificationService).createFriendAcceptedNotification(
                REQUESTER_ID,
                ACCEPTER_NICKNAME,
                FRIEND_ID
        );

        verify(notificationRetryService).save(
                eq(EVENT_ID),
                eq(OUTBOX_ID),
                eq(REQUESTER_ID),
                eq(NotificationType.FRIEND_ACCEPTED),
                same(event),
                eq(notificationErrorMessage)
        );
    }
}