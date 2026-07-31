package com.example.schedulebook.domain.auth.processor;

import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.auth.service.AuditLogService;
import com.example.schedulebook.domain.outbox.service.OutboxTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogRetryProcessor  {
    private final AuditLogService auditLogService;
    private final OutboxTransactionService outboxTransactionService;

    public void process(Long outboxId, AuditEvent event) {
        try {
            auditLogService.save(event);

        } catch (Exception e) {
            log.error("AuditEvent 처리 실패 outboxId = {}, eventType = {}", outboxId, event.eventType());

            outboxTransactionService.handleFailure(outboxId, e);
        }
    }
}
