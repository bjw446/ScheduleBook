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

                try {
                    notificationRetryService.markSuccess(notificationRetry.getId());

                } catch (Exception ex) {
                    log.error("알림 재시도 성공 상태 변경 실패 notificationRetryId = {}", notificationRetry.getId(), ex);
                }

            } catch (Exception e) {
                log.warn("알림 재시도 처리 실패 notificationRetryId = {}", notificationRetry.getId(), e);

                try {
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

                } catch (Exception exception) {
                    log.error("알림 재시도 상태 갱신 실패 notificationRetryId = {}", notificationRetry.getId(), exception);
                }
            }
        }
    }
}
