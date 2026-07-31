package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.auth.processor.AuditLogRetryProcessor;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogEventOutboxHandler implements OutboxEventHandler<AuditEvent> {
    private final AuditLogRetryProcessor auditLogRetryProcessor;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.AUDIT_EVENT;
    }

    @Override
    public Class<AuditEvent> payloadType() {
        return AuditEvent.class;
    }

    @Override
    public void handle(Long outboxId, AuditEvent payload) {
        auditLogRetryProcessor.process(outboxId, payload);
    }
}
