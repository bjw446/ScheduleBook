package com.example.schedulebook.common.redis.subscriber;

import com.example.schedulebook.common.redis.service.RedisEventDeduplicationService;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.auth.handler.ForceLogoutHandler;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import com.example.schedulebook.domain.comment.subscriber.CommentSubscriber;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisSubscriberTest {

    private SimpMessagingTemplate messagingTemplate;
    private CommentSubscriber commentSubscriber;
    private ForceLogoutHandler forceLogoutHandler;
    private RedisPresenceService redisPresenceService;
    private RedisEventDeduplicationService redisEventDeduplicationService;

    private RedisSubscriber redisSubscriber;

    @BeforeEach
    void setUp() {

        messagingTemplate =
                mock(SimpMessagingTemplate.class);

        commentSubscriber =
                mock(CommentSubscriber.class);

        forceLogoutHandler =
                mock(ForceLogoutHandler.class);

        redisPresenceService =
                mock(RedisPresenceService.class);

        redisEventDeduplicationService =
                mock(RedisEventDeduplicationService.class);

        redisSubscriber =
                new RedisSubscriber(
                        messagingTemplate,
                        commentSubscriber,
                        forceLogoutHandler,
                        redisPresenceService,
                        redisEventDeduplicationService
                );
    }

    @Test
    @DisplayName("알림 수신자가 온라인이면 WebSocket 알림을 전송한다")
    void givenOnlineReceiver_whenOnNotification_thenSendWebSocketNotification() {

        // given
        Long receiverId =
                1L;

        NotificationEventResponse event =
                mock(NotificationEventResponse.class);

        when(
                event.receiverId()
        ).thenReturn(
                receiverId
        );

        when(
                redisPresenceService.isOnline(
                        receiverId
                )
        ).thenReturn(
                true
        );

        // when
        redisSubscriber.onNotification(
                event
        );

        // then
        verify(
                redisPresenceService
        ).isOnline(
                receiverId
        );

        verify(
                messagingTemplate
        ).convertAndSendToUser(
                eq(receiverId.toString()),
                eq("/queue/notification"),
                eq(event)
        );
    }

    @Test
    @DisplayName("알림 수신자가 오프라인이면 WebSocket 알림을 전송하지 않는다")
    void givenOfflineReceiver_whenOnNotification_thenDoNotSendNotification() {

        // given
        Long receiverId =
                1L;

        NotificationEventResponse event =
                mock(NotificationEventResponse.class);

        when(
                event.receiverId()
        ).thenReturn(
                receiverId
        );

        when(
                redisPresenceService.isOnline(
                        receiverId
                )
        ).thenReturn(
                false
        );

        // when
        redisSubscriber.onNotification(
                event
        );

        // then
        verify(
                redisPresenceService
        ).isOnline(
                receiverId
        );

        verifyNoInteractions(
                messagingTemplate
        );
    }

    @Test
    @DisplayName("receiverId가 null이면 알림을 전송하지 않는다")
    void givenNullReceiverId_whenOnNotification_thenDoNotSendNotification() {

        // given
        NotificationEventResponse event =
                mock(NotificationEventResponse.class);

        when(
                event.receiverId()
        ).thenReturn(
                null
        );

        // when
        redisSubscriber.onNotification(
                event
        );

        // then
        verifyNoInteractions(
                redisPresenceService,
                messagingTemplate
        );
    }

    @Test
    @DisplayName("WebSocket 전송 중 예외가 발생해도 예외를 전파하지 않는다")
    void givenWebSocketException_whenOnNotification_thenNotPropagateException() {

        // given
        Long receiverId =
                1L;

        NotificationEventResponse event =
                mock(NotificationEventResponse.class);

        when(
                event.receiverId()
        ).thenReturn(
                receiverId
        );

        when(
                redisPresenceService.isOnline(
                        receiverId
                )
        ).thenReturn(
                true
        );

        doThrow(
                new RuntimeException("websocket error")
        ).when(
                messagingTemplate
        ).convertAndSendToUser(
                anyString(),
                anyString(),
                any()
        );

        // when & then
        assertDoesNotThrow(
                () -> redisSubscriber.onNotification(
                        event
                )
        );

        verify(
                messagingTemplate
        ).convertAndSendToUser(
                eq(receiverId.toString()),
                eq("/queue/notification"),
                eq(event)
        );
    }

    @Test
    @DisplayName("댓글 이벤트를 수신하면 CommentSubscriber에 위임한다")
    void givenCommentEvent_whenOnComment_thenDelegateToCommentSubscriber() {

        // given
        CommentEvent event =
                mock(CommentEvent.class);

        // when
        redisSubscriber.onComment(
                event
        );

        // then
        verify(
                commentSubscriber
        ).onComment(
                event
        );
    }

    @Test
    @DisplayName("강제 로그아웃 이벤트가 처음 수신되면 로그아웃 처리를 수행한다")
    void givenNewForceLogoutEvent_whenOnForceLogout_thenHandleEvent() {

        // given
        ForceLogoutSessionEvent event =
                mock(ForceLogoutSessionEvent.class);

        String eventId =
                "event-1";

        when(
                event.eventId()
        ).thenReturn(
                eventId
        );

        when(
                redisEventDeduplicationService.isAlreadyProcessed(
                        eventId
                )
        ).thenReturn(
                false
        );

        // when
        redisSubscriber.onForceLogout(
                event
        );

        // then
        verify(
                redisEventDeduplicationService
        ).isAlreadyProcessed(
                eventId
        );

        verify(
                forceLogoutHandler
        ).handle(
                event
        );
    }

    @Test
    @DisplayName("이미 처리된 강제 로그아웃 이벤트는 무시한다")
    void givenAlreadyProcessedEvent_whenOnForceLogout_thenDoNotHandleEvent() {

        // given
        ForceLogoutSessionEvent event =
                mock(ForceLogoutSessionEvent.class);

        String eventId =
                "event-1";

        when(
                event.eventId()
        ).thenReturn(
                eventId
        );

        when(
                redisEventDeduplicationService.isAlreadyProcessed(
                        eventId
                )
        ).thenReturn(
                true
        );

        // when
        redisSubscriber.onForceLogout(
                event
        );

        // then
        verify(
                redisEventDeduplicationService
        ).isAlreadyProcessed(
                eventId
        );

        verify(
                forceLogoutHandler,
                never()
        ).handle(
                any()
        );
    }
}
