package com.example.schedulebook.domain.friend.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestedEvent;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notificationretry.service.ProcessedNotificationRetryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendRetryProcessorTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProcessedNotificationRetryService processedNotificationRetryService;

    @Mock
    private NotificationRetry notificationRetry;

    @Mock
    private FriendAcceptedEvent friendAcceptedEvent;

    @Mock
    private FriendRequestedEvent friendRequestedEvent;

    private FriendRetryProcessor processor;

    private static final Long RECEIVER_ID = 1L;
    private static final Long FRIEND_ID = 10L;

    private static final String ACCEPTED_PAYLOAD = "friend-accepted-payload";
    private static final String REQUESTED_PAYLOAD = "friend-requested-payload";

    private static final String ACCEPTER_NICKNAME = "accepter";
    private static final String REQUESTER_NICKNAME = "requester";

    @BeforeEach
    void setUp() {
        processor = new FriendRetryProcessor(
                objectMapper,
                notificationService,
                processedNotificationRetryService
        );

        when(processedNotificationRetryService.prepareProcessedNotificationRetry(
                notificationRetry
        )).thenReturn(false);
    }

    @Test
    void 이미_처리된_알림이면_재처리하지_않는다() {
        // given
        when(processedNotificationRetryService.prepareProcessedNotificationRetry(
                notificationRetry
        )).thenReturn(true);

        // when
        processor.process(notificationRetry);

        // then
        verify(processedNotificationRetryService)
                .prepareProcessedNotificationRetry(notificationRetry);

        verifyNoInteractions(objectMapper, notificationService);
        verifyNoMoreInteractions(processedNotificationRetryService);
    }

    @Test
    void FRIEND_ACCEPTED_알림이면_FriendAcceptedEvent로_역직렬화하여_알림을_생성한다()
            throws Exception {
        // given
        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.FRIEND_ACCEPTED);
        when(notificationRetry.getPayload())
                .thenReturn(ACCEPTED_PAYLOAD);
        when(notificationRetry.getReceiverId())
                .thenReturn(RECEIVER_ID);

        when(objectMapper.readValue(
                ACCEPTED_PAYLOAD,
                FriendAcceptedEvent.class
        )).thenReturn(friendAcceptedEvent);

        when(friendAcceptedEvent.accepterNickname())
                .thenReturn(ACCEPTER_NICKNAME);
        when(friendAcceptedEvent.friendId())
                .thenReturn(FRIEND_ID);

        // when
        processor.process(notificationRetry);

        // then
        verify(processedNotificationRetryService)
                .prepareProcessedNotificationRetry(notificationRetry);

        verify(notificationRetry).getNotificationType();
        verify(notificationRetry).getPayload();
        verify(notificationRetry).getReceiverId();

        verify(objectMapper).readValue(
                ACCEPTED_PAYLOAD,
                FriendAcceptedEvent.class
        );

        verify(friendAcceptedEvent).accepterNickname();
        verify(friendAcceptedEvent).friendId();

        verify(notificationService).createFriendAcceptedNotification(
                RECEIVER_ID,
                ACCEPTER_NICKNAME,
                FRIEND_ID
        );

        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void FRIEND_REQUEST_알림이면_FriendRequestedEvent로_역직렬화하여_알림을_생성한다()
            throws Exception {
        // given
        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.FRIEND_REQUEST);
        when(notificationRetry.getPayload())
                .thenReturn(REQUESTED_PAYLOAD);
        when(notificationRetry.getReceiverId())
                .thenReturn(RECEIVER_ID);

        when(objectMapper.readValue(
                REQUESTED_PAYLOAD,
                FriendRequestedEvent.class
        )).thenReturn(friendRequestedEvent);

        when(friendRequestedEvent.requesterNickname())
                .thenReturn(REQUESTER_NICKNAME);
        when(friendRequestedEvent.friendId())
                .thenReturn(FRIEND_ID);

        // when
        processor.process(notificationRetry);

        // then
        verify(processedNotificationRetryService)
                .prepareProcessedNotificationRetry(notificationRetry);

        verify(notificationRetry).getNotificationType();
        verify(notificationRetry).getPayload();
        verify(notificationRetry).getReceiverId();

        verify(objectMapper).readValue(
                REQUESTED_PAYLOAD,
                FriendRequestedEvent.class
        );

        verify(friendRequestedEvent).requesterNickname();
        verify(friendRequestedEvent).friendId();

        verify(notificationService).createFriendRequestNotification(
                RECEIVER_ID,
                REQUESTER_NICKNAME,
                FRIEND_ID
        );

        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void 지원하지_않는_알림_타입이면_invalid_notification_type_예외가_발생한다() {
        // given
        NotificationType unsupportedType = findUnsupportedNotificationType();

        Assumptions.assumeTrue(
                unsupportedType != null,
                "FRIEND_ACCEPTED와 FRIEND_REQUEST 외의 NotificationType이 필요합니다."
        );

        when(notificationRetry.getNotificationType())
                .thenReturn(unsupportedType);

        // when & then
        assertThatThrownBy(() ->
                processor.process(notificationRetry)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_NOTIFICATION_TYPE);

        verify(processedNotificationRetryService)
                .prepareProcessedNotificationRetry(notificationRetry);

        verify(notificationRetry).getNotificationType();

        verifyNoInteractions(objectMapper, notificationService);
    }

    @Test
    void FRIEND_ACCEPTED_payload_역직렬화에_실패하면_json_deserialization_failed_예외가_발생한다()
            throws Exception {
        // given
        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.FRIEND_ACCEPTED);
        when(notificationRetry.getPayload())
                .thenReturn(ACCEPTED_PAYLOAD);

        when(objectMapper.readValue(
                ACCEPTED_PAYLOAD,
                FriendAcceptedEvent.class
        )).thenThrow(new JsonProcessingException("invalid payload") {
        });

        // when & then
        assertThatThrownBy(() ->
                processor.process(notificationRetry)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.JSON_DESERIALIZATION_FAILED);

        verify(objectMapper).readValue(
                ACCEPTED_PAYLOAD,
                FriendAcceptedEvent.class
        );

        verifyNoInteractions(notificationService);
    }

    @Test
    void FRIEND_REQUEST_payload_역직렬화에_실패하면_json_deserialization_failed_예외가_발생한다()
            throws Exception {
        // given
        when(notificationRetry.getNotificationType())
                .thenReturn(NotificationType.FRIEND_REQUEST);
        when(notificationRetry.getPayload())
                .thenReturn(REQUESTED_PAYLOAD);

        when(objectMapper.readValue(
                REQUESTED_PAYLOAD,
                FriendRequestedEvent.class
        )).thenThrow(new JsonProcessingException("invalid payload") {
        });

        // when & then
        assertThatThrownBy(() ->
                processor.process(notificationRetry)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.JSON_DESERIALIZATION_FAILED);

        verify(objectMapper).readValue(
                REQUESTED_PAYLOAD,
                FriendRequestedEvent.class
        );

        verifyNoInteractions(notificationService);
    }

    private NotificationType findUnsupportedNotificationType() {
        return Arrays.stream(NotificationType.values())
                .filter(type ->
                        type != NotificationType.FRIEND_ACCEPTED
                                && type != NotificationType.FRIEND_REQUEST
                )
                .findFirst()
                .orElse(null);
    }
}