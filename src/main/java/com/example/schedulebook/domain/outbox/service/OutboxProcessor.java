package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.publisher.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxProcessor {
    private final OutboxPublisher outboxPublisher;
    private final OutboxTransactionService outboxTransactionService;

    public void processPendingOutbox() {
        outboxTransactionService.recoverStuck();

        List<Long> outboxIds = outboxTransactionService.claimOutboxes();

        for (Long outboxId : outboxIds) {
            try {
                Outbox outbox = outboxTransactionService.findById(outboxId);

                outboxPublisher.publish(outbox);

                outboxTransactionService.markSuccess(outboxId);

            } catch (Exception e) {
                outboxTransactionService.handleFailure(outboxId, e);
            }
        }
    }
}
