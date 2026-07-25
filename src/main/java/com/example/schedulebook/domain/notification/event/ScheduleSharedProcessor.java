package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationPublishService;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.scheduleshare.event.ScheduleSharedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleSharedProcessor implements NotificationEventProcessor<ScheduleSharedEvent> {
    private final NotificationService notificationService;
    private final NotificationPublishService notificationPublishService;

    @Override
    public Class<ScheduleSharedEvent> supports() {
        return ScheduleSharedEvent.class;
    }

    @Override
    public void process(ScheduleSharedEvent event) {
        notificationService.createScheduleSharedNotification(event.receiverId(), event.ownerNickname(), event.shareId());

        notificationPublishService.publish(event.receiverId(), NotificationType.SCHEDULE_SHARED, event.ownerNickname());
    }
}
