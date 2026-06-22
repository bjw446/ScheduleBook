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
        log.info("Redis 이벤트 발행 = {}", event);

        messagingTemplate.convertAndSend("/topic/notification/" + event.receiverId(), event);
    }
}
