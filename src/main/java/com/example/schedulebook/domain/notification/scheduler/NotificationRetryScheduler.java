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
        for (int batch = 0; batch < CommonConst.MAX_BATCHES_PER_RUN; batch++) {
            List<NotificationRetry> notificationRetries = notificationRetryService.findRetryTargets(CommonConst.BATCH_SIZE);

            if (notificationRetries.isEmpty()) {
                break;
            }

            for (NotificationRetry notificationRetry : notificationRetries) {
                String claimToken = notificationRetryService.markProcessing(notificationRetry.getId());

                if (claimToken == null) {
                    continue;
                }

                try {
                    notificationRetryProcessor.dispatch(notificationRetry.getId());

                    notificationRetryService.markSuccess(notificationRetry.getId(), claimToken);

                } catch (Exception e) {
                    log.warn("알림 재시도 처리 실패 notificationRetryId = {}", notificationRetry.getId(), e);

                    try {
                        if ((notificationRetry.getRetryCount() + 1) >= CommonConst.MAX_RETRY) {
                            notificationRetryService.markFailed(
                                    notificationRetry.getId(),
                                    e.getMessage(),
                                    claimToken
                            );

                        } else {
                            notificationRetryService.markRetry(
                                    notificationRetry.getId(),
                                    e.getMessage(),
                                    notificationRetry.getRetryCount(),
                                    claimToken
                            );
                        }

                    } catch (Exception exception) {
                        log.error("알림 재시도 상태 갱신 실패 notificationRetryId = {}", notificationRetry.getId(), exception);
                    }
                }
            }
        }
    }
}
