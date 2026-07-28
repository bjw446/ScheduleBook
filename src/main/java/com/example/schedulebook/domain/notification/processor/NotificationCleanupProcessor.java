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

    public boolean process(Long outboxId, Long userId) {
        return loggingExecutor.execute(
                outboxId,
                "알림 삭제",
                () -> notificationService.deleteAllNotifications(userId)
        );
    }
}
