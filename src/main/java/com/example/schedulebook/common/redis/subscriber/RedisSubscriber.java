package com.example.schedulebook.common.redis.subscriber;

import com.example.schedulebook.common.websocket.WebSocketSessionRegistry;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.auth.service.ForceLogoutHandler;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import com.example.schedulebook.domain.comment.subscriber.CommentSubscriber;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber {
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final CommentSubscriber commentSubscriber;
    private final ForceLogoutHandler forceLogoutHandler;

    public void onNotification(NotificationEventResponse event) {
        Long receiverId = event.receiverId();

        if (receiverId == null) {
            log.warn("receiverId가 null인 알림 이벤트 무시, event = {}", event);

            return;
        }

        if (!webSocketSessionRegistry.isOnline(receiverId)) {
            log.info("사용자 {} 오프라인 상태, 실시간 전송 생략", receiverId);

            return;
        }

        log.info("Redis 이벤트 발행 = {}", event);

        try {
            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/notification",
                    event
            );

            log.info("알림 전송 userId = {}, eventType = {}, unreadCount = {}", receiverId, event.eventType(), event.unreadCount());
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패, receiverId = {}, event = {}", receiverId, event, e);
        }
    }

    public void onComment(CommentEvent event) {
        commentSubscriber.onComment(event);
    }

    public void onForceLogout(ForceLogoutSessionEvent event) {
        forceLogoutHandler.handle(event);
    }
}
