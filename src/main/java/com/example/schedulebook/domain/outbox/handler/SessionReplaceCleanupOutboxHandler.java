package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.auth.event.ReplaceSessionCleanupEvent;
import com.example.schedulebook.domain.auth.service.SessionService;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionReplaceCleanupOutboxHandler implements OutboxEventHandler<ReplaceSessionCleanupEvent> {
    private final SessionService sessionService;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.SESSION_REPLACE_CLEANUP;
    }

    @Override
    public Class<ReplaceSessionCleanupEvent> payloadType() {
        return ReplaceSessionCleanupEvent.class;
    }

    @Override
    public void handle(Long outboxId, ReplaceSessionCleanupEvent payload) {
        sessionService.cleanupReplacedSession(payload.oldSessionId());
    }
}
