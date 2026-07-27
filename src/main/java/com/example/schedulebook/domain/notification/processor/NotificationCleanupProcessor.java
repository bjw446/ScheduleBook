package com.example.schedulebook.domain.notification.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationCleanupProcessor {
    private final NotificationService notificationService;
    private final LoggingExecutor loggingExecutor;

    public void process(Long outboxId, Long userId) {
        loggingExecutor.execute("알림 삭제", () -> notificationService.deleteAllNotifications(userId));
    }
}
