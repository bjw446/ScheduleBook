package com.example.schedulebook.domain.scheduleshare.processor;

import com.example.schedulebook.domain.notification.processor.NotificationEventProcessor;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.scheduleshare.event.ScheduleSharedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleSharedProcessor implements NotificationEventProcessor<ScheduleSharedEvent> {
    private final NotificationService notificationService;

    @Override
    public Class<ScheduleSharedEvent> supports() {
        return ScheduleSharedEvent.class;
    }

    @Override
    public void process(Long outboxId, ScheduleSharedEvent event) {
        notificationService.createScheduleSharedNotification(event.receiverId(), event.ownerNickname(), event.shareId());
    }
}
