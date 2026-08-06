package com.example.schedulebook.domain.deadletter.service;

import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterRetryService {
    private final DeadLetterService deadLetterService;


    public void saveDeadLetterWithRetry(
            DeadLetterType deadLetterType,
            DeadLetterSource deadLetterSource,
            DeadLetterAggregateType deadLetterAggregateType,
            String aggregateId,
            Long userId,
            String payload,
            Exception e
    ) {
        for (int i = 0; i < 3; i++) {
            try {
                deadLetterService.save(
                        deadLetterType,
                        deadLetterSource,
                        deadLetterAggregateType,
                        aggregateId,
                        userId,
                        payload,
                        e.getMessage(),
                        e.getClass().getSimpleName(),
                        i + 1
                );

                return;

            } catch (Exception exception) {
                log.warn("{} DLQ 저장 재시도 {} / 3", deadLetterSource, i + 1, exception);

                if (i < 2) {
                    try {
                        Thread.sleep((1L << i) * 100);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();

                        log.warn("{} DLQ 저장 재시도 중 인터럽트 발생", deadLetterSource);

                        return;
                    }
                }
            }
        }

        log.error("{} DLQ 저장 최종 실패, payload = {}", deadLetterSource, normalizePayload(payload));
    }

    private String normalizePayload(String payload) {
        if (payload == null) {
            return "Unknown payload";
        }

        return payload.length() > 500 ? payload.substring(0, 500) : payload;
    }
}
