package com.example.schedulebook.common.redis.subscriber;

import com.example.schedulebook.common.redis.service.RedisEventDeduplicationService;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.auth.handler.ForceLogoutHandler;
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
    private final CommentSubscriber commentSubscriber;
    private final ForceLogoutHandler forceLogoutHandler;
    private final RedisPresenceService redisPresenceService;
    private final RedisEventDeduplicationService redisEventDeduplicationService;

    public void onNotification(NotificationEventResponse event) {
        Long receiverId = event.receiverId();

        if (receiverId == null) {
            log.warn("receiverId가 null인 알림 이벤트 무시, event = {}", event);

            return;
        }

        if (!redisPresenceService.isOnline(receiverId)) {
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
        if (redisEventDeduplicationService.isAlreadyProcessed(event.eventId())) {
            log.info("중복 Redis 이벤트 무시 eventId = {}", event.eventId());

            return;
        }

        forceLogoutHandler.handle(event);
    }
}