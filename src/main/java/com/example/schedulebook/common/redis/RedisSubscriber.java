package com.example.schedulebook.common.redis;

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

    public void onMessage(NotificationRealtimeResponse event) {
        if (event.receiverId() == null) {
            log.warn("receiverId가 null인 알림 이벤트 무시, event = {}", event);

            return;
        }

        log.info("Redis 이벤트 발행 = {}", event);

        try {
            messagingTemplate.convertAndSendToUser(
                    event.receiverId().toString(),
                    "/queue/notification",
                    event);
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패, receiverId : {}, event : {}", event.receiverId(), event, e);
        }
    }
}
