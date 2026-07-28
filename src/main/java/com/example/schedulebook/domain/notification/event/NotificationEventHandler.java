package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.processor.NotificationEventProcessor;
import com.example.schedulebook.domain.notification.processor.NotificationProcessorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventHandler {
    private final NotificationProcessorRegistry notificationProcessorRegistry;

    @EventListener
    public void handle(NotificationOutboxEvent event) {
        NotificationEventProcessor<NotificationEventMarker> notificationEventProcessor =
                notificationProcessorRegistry.get(event.payload());

        if (notificationEventProcessor == null) {
            throw new BaseException(ErrorEnum.NOTIFICATION_EVENT_NOT_FOUND);
        }

        notificationEventProcessor.process(event.outboxId(), event.payload());
    }
}
