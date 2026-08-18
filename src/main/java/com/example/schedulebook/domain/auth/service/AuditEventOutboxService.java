package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditEventOutboxService {
    private final OutboxService outboxService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveReplayEvent(Long userId, String loginId, String ip, String userAgent) {
        String eventId = UUID.randomUUID().toString();

        outboxService.save(
                eventId,
                OutboxAggregateType.USER,
                String.valueOf(userId),
                OutboxEventType.AUDIT_EVENT,
                new AuditEvent(
                        eventId,
                        userId,
                        null,
                        loginId,
                        AuditEventType.REFRESH_REPLAY,
                        ip,
                        userAgent
                )
        );
    }
}
