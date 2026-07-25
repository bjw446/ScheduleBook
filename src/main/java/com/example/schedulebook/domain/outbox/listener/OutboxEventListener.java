package com.example.schedulebook.domain.outbox.listener;

import com.example.schedulebook.domain.outbox.event.OutboxSaveEvent;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OutboxEventListener {
    private final OutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void saveOutbox(OutboxSaveEvent event) {
        outboxService.save(
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.payload()
        );
    }
}
