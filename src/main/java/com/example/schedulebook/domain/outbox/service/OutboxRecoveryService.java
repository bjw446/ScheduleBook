package com.example.schedulebook.domain.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRecoveryService {
    private final OutboxTransactionService outboxTransactionService;

    public void recoverStuckOutboxes() {
        log.debug("Stuck Outbox 복구 시작");

        outboxTransactionService.recoverStuck();

        log.debug("Stuck Outbox 복구 종료");
    }
}
