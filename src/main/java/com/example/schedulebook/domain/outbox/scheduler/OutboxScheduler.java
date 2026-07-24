package com.example.schedulebook.domain.outbox.scheduler;

import com.example.schedulebook.domain.outbox.service.OutboxProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final OutboxProcessor outboxProcessor;

    @Scheduled(fixedDelay = 3000)
    public void process() {
        outboxProcessor.processPendingOutbox();
    }
}
