package com.example.schedulebook.domain.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventHandler {
    private final NotificationProcessorRegistry notificationProcessorRegistry;

    @Async
    @EventListener
    public void handle(NotificationEventMarker event) {
        try {
            NotificationEventProcessor<NotificationEventMarker> notificationEventProcessor =
                    notificationProcessorRegistry.get(event);

            if (notificationEventProcessor != null) {
                notificationEventProcessor.process(event);
            }

        } catch (Exception e) {
            log.error("Notification 처리 실패, event = {}", event.getClass().getSimpleName(), e);

            // TODO Outbox 재시도/실패 상태 기록 연결
        }
    }
}
