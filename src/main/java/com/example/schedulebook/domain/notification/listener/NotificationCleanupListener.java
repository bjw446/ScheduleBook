package com.example.schedulebook.domain.notification.listener;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationCleanupListener {
    private final NotificationService notificationService;
    private final LoggingExecutor loggingExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserWithdrawEvent event) {
        loggingExecutor.execute("알림 삭제", () -> notificationService.deleteAllNotifications(event.userId()));
    }
}
