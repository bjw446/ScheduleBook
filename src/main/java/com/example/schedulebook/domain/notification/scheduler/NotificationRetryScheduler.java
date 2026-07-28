package com.example.schedulebook.domain.notification.scheduler;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.domain.notification.entity.NotificationRetry;
import com.example.schedulebook.domain.notification.processor.NotificationRetryProcessor;
import com.example.schedulebook.domain.notification.service.NotificationRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {
    private final NotificationRetryProcessor notificationRetryProcessor;
    private final NotificationRetryService notificationRetryService;

    @Scheduled(fixedDelay = 30_000)
    public void process() {
        List<NotificationRetry> notificationRetries = notificationRetryService.findRetryTargets();

        for (NotificationRetry notificationRetry : notificationRetries) {
            if (!notificationRetryService.markProcessing(notificationRetry.getId())) {
                continue;
            }

            try {
                notificationRetryProcessor.dispatch(notificationRetry);

                notificationRetryService.markSuccess(notificationRetry.getId());

            } catch (Exception e) {
                if ((notificationRetry.getRetryCount() + 1) >= CommonConst.MAX_RETRY) {
                    notificationRetryService.markFailed(
                            notificationRetry.getId(),
                            e.getMessage()
                    );

                } else {
                    notificationRetryService.markRetry(
                            notificationRetry.getId(),
                            e.getMessage(),
                            notificationRetry.getRetryCount()
                    );
                }
            }
        }
    }
}
