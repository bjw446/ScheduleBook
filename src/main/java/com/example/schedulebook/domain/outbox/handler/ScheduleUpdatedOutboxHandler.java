package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.schedule.event.ScheduleUpdatedEvent;
import com.example.schedulebook.domain.scheduleshare.service.ScheduleShareUpdateManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleUpdatedOutboxHandler implements OutboxEventHandler<ScheduleUpdatedEvent> {
    private final ScheduleShareUpdateManager scheduleShareUpdateManager;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.SCHEDULE_UPDATED;
    }

    @Override
    public Class<ScheduleUpdatedEvent> payloadType() {
        return ScheduleUpdatedEvent.class;
    }

    @Override
    public void handle(Long outboxId, ScheduleUpdatedEvent payload) {
        scheduleShareUpdateManager.handleUpdated(payload.scheduleId());
    }
}
