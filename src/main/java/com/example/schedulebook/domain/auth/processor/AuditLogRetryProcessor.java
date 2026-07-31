package com.example.schedulebook.domain.auth.processor;

import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.auth.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogRetryProcessor  {
    private final AuditLogService auditLogService;
    private final RefreshReplayDetectedProcessor refreshReplayDetectedProcessor;

    public void process(Long outboxId, AuditEvent event) {
        try {
            auditLogService.save(event);

            if (event.eventType() == AuditEventType.REFRESH_REPLAY) {
                refreshReplayDetectedProcessor.process(event);
            }

        } catch (Exception e) {
            log.error("AuditEvent 처리 실패 outboxId = {}, eventType = {}", outboxId, event.eventType());

            throw e;
        }
    }
}
