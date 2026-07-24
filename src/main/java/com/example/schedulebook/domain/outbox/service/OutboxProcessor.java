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

        List<Outbox> outboxes = outboxTransactionService.claimOutboxes();

        for (Outbox outbox : outboxes) {
            try {
                outboxPublisher.publish(outbox);

                outboxTransactionService.markSuccess(outbox);

            } catch (Exception e) {
                outboxTransactionService.handleFailure(outbox, e);
            }
        }
    }
}
