package com.example.schedulebook.domain.notificationretry.service;

import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationRetryStateService {
    private final NotificationRetryService notificationRetryService;
    private final ProcessedNotificationRetryService processedNotificationRetryService;

    @Transactional
    public void completeFailure(NotificationRetry notificationRetry, String reason, String claimToken) {
        notificationRetryService.markFailed(notificationRetry.getId(), reason, claimToken);

        processedNotificationRetryService.markFailed(notificationRetry.getOutboxId(), notificationRetry.getReceiverId());
    }

    @Transactional
    public void completeSuccess(NotificationRetry notificationRetry, String claimToken) {
        notificationRetryService.markSuccess(notificationRetry.getId(), claimToken);

        processedNotificationRetryService.markSuccess(notificationRetry.getOutboxId(), notificationRetry.getReceiverId());
    }
}
