package com.example.schedulebook.domain.notificationretry.scheduler;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.metrics.RetrySchedulerMetrics;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notificationretry.processor.NotificationRetryProcessor;
import com.example.schedulebook.domain.notificationretry.repository.NotificationRetryRepository;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryStateService;
import jakarta.annotation.PostConstruct;
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
    private final NotificationRetryStateService notificationRetryStateService;
    private final NotificationRetryRepository notificationRetryRepository;
    private final RetrySchedulerMetrics retrySchedulerMetrics;
    private static final String METRIC = "notification";

    @Scheduled(fixedDelay = 30_000)
    public void process() {
        retrySchedulerMetrics.schedulerRun(METRIC);

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

                retrySchedulerMetrics.processed(METRIC);

                try {
                    notificationRetryProcessor.dispatch(notificationRetry.getId());

                    notificationRetryStateService.completeSuccess(notificationRetry, claimToken);

                    retrySchedulerMetrics.success(METRIC);

                } catch (Exception e) {
                    retrySchedulerMetrics.error(METRIC);

                    log.warn("알림 재시도 처리 실패 notificationRetryId = {}", notificationRetry.getId(), e);

                    retry(notificationRetry, claimToken, e);
                }
            }
        }
    }

    @PostConstruct
    public void registerMetrics() {
        retrySchedulerMetrics.registerPendingGauge(METRIC, notificationRetryRepository::countPending);
    }

    private void retry(NotificationRetry notificationRetry, String claimToken, Exception e) {
        try {
            if ((notificationRetry.getRetryCount() + 1) >= CommonConst.MAX_RETRY) {
                try {
                    notificationRetryStateService.completeFailure(notificationRetry, e.getMessage(), claimToken, e);

                    retrySchedulerMetrics.dlq(METRIC);

                } catch (Exception exception) {
                    retrySchedulerMetrics.error(METRIC);

                    log.error("알림 재시도 FAILED 상태 갱신 실패 notificationRetryId = {}",
                            notificationRetry.getId(),
                            exception
                    );
                }

            } else {
                notificationRetryService.markRetry(
                        notificationRetry.getId(),
                        e.getMessage(),
                        notificationRetry.getRetryCount(),
                        claimToken
                );

                retrySchedulerMetrics.retry(METRIC);
            }

        } catch (Exception exception) {
            retrySchedulerMetrics.error(METRIC);

            log.error("알림 재시도 상태 갱신 실패 notificationRetryId = {}", notificationRetry.getId(), exception);
        }
    }
}
