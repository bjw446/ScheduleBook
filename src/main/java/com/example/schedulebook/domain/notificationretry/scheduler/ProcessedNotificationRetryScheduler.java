package com.example.schedulebook.domain.notificationretry.scheduler;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.domain.notificationretry.repository.ProcessedNotificationRetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessedNotificationRetryScheduler {
    private final ProcessedNotificationRetryRepository processedNotificationRetryRepository;

    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void recoverTimeoutProcessing() {
        int count = processedNotificationRetryRepository.recoverTimeoutProcessing(
                LocalDateTime.now().minusMinutes(CommonConst.PROCESSING_TIMEOUT_MINUTES)
        );

        if (count > 0) {
            log.info("오래된 Processing {} 건 재시도 처리", count);
        }
    }
}
