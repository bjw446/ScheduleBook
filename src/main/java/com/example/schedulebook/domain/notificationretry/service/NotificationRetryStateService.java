package com.example.schedulebook.domain.notificationretry.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryStateService {
    private final NotificationRetryService notificationRetryService;
    private final ProcessedNotificationRetryService processedNotificationRetryService;
    private final DeadLetterService deadLetterService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeFailure(NotificationRetry notificationRetry, String reason, String claimToken, Exception e) {
        notificationRetryService.markFailed(notificationRetry.getId(), reason, claimToken);

        processedNotificationRetryService.markFailed(
                notificationRetry.getOutboxId(),
                notificationRetry.getReceiverId(),
                claimToken
        );

        try {
            deadLetterService.save(
                    DeadLetterType.NOTIFICATION_RETRY,
                    DeadLetterSource.NOTIFICATION_RETRY_SCHEDULER,
                    DeadLetterAggregateType.NOTIFICATION_RETRY,
                    String.valueOf(notificationRetry.getId()),
                    notificationRetry.getReceiverId(),
                    notificationRetry.getPayload(),
                    e.getMessage(),
                    e.getClass().getSimpleName(),
                    notificationRetry.getRetryCount() + 1,
                    notificationRetry.getEventId()
            );

        } catch (Exception dlqException) {
        log.error("DLQ 저장 실패", dlqException);

        throw new BaseException(ErrorEnum.DEAD_LETTER_SAVE_FAILED, dlqException);
        }
    }

    @Transactional
    public void completeSuccess(NotificationRetry notificationRetry, String claimToken) {
        notificationRetryService.markSuccess(notificationRetry.getId(), claimToken);

        processedNotificationRetryService.markSuccess(
                notificationRetry.getOutboxId(),
                notificationRetry.getReceiverId(),
                claimToken
        );
    }
}
