package com.example.schedulebook.domain.deadletter.scheduler;

import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterScheduler {
    private final DeadLetterService deadLetterService;

    @Scheduled(fixedDelay = 60_000)
    public void reclaimExpiredProcessing() {
        int reclaim = deadLetterService.reclaimExpiredProcessing();

        if (reclaim > 0) {
            log.warn("만료된 DeadLetter PROCESSING 상태 {}건 회수", reclaim);
        }
    }
}
