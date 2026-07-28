package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.schedule.event.ScheduleCanceledEvent;
import com.example.schedulebook.domain.scheduleshare.service.ScheduleShareUpdateManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleCanceledOutboxHandler implements OutboxEventHandler<ScheduleCanceledEvent> {
    private final ScheduleShareUpdateManager scheduleShareUpdateManager;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.SCHEDULE_CANCELED;
    }

    @Override
    public Class<ScheduleCanceledEvent> payloadType() {
        return ScheduleCanceledEvent.class;
    }

    @Override
    public void handle(Long outboxId, ScheduleCanceledEvent payload) {
        scheduleShareUpdateManager.handleCanceled(payload.scheduleId(), payload.sharedUserId());
    }
}
