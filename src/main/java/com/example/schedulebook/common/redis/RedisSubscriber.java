package com.example.schedulebook.common.redis;

import com.example.schedulebook.common.websocket.WebSocketSessionRegistry;
import com.example.schedulebook.domain.notification.dto.response.NotificationRealtimeResponse;
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

    public void onMessage(NotificationRealtimeResponse event) {
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
                    event);
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패, receiverId : {}, event : {}", receiverId, event, e);
        }
    }
}
