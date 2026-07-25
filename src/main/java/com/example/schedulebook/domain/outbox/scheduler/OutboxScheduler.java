package com.example.schedulebook.domain.outbox.scheduler;

import com.example.schedulebook.domain.outbox.service.OutboxProcessor;
import com.example.schedulebook.domain.outbox.service.OutboxRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {
    private final OutboxProcessor outboxProcessor;
    private final OutboxRecoveryService outboxRecoveryService;

    @Scheduled(fixedDelay = 3000)
    public void process() {
        try {
            log.debug("Outbox Scheduler 실행");

            outboxProcessor.processPendingOutbox();

        } catch (Exception e) {
            log.error("Outbox Scheduler 실행 실패", e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void recover() {
        try {
            outboxRecoveryService.recoverStuckOutboxes();

        } catch (Exception e) {
            log.error("Outbox 복구 실패", e);
        }
    }
}
