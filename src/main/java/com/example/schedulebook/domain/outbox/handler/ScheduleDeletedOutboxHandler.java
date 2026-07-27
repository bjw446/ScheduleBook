package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.schedule.event.ScheduleDeletedEvent;
import com.example.schedulebook.domain.scheduleshare.service.ScheduleShareUpdateManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleDeletedOutboxHandler implements OutboxEventHandler<ScheduleDeletedEvent> {
    private final ScheduleShareUpdateManager scheduleShareUpdateManager;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.SCHEDULE_DELETED;
    }

    @Override
    public Class<ScheduleDeletedEvent> payloadType() {
        return ScheduleDeletedEvent.class;
    }

    @Override
    public void handle(Long outboxId, ScheduleDeletedEvent payload) {
        scheduleShareUpdateManager.handleDeleted(payload.scheduleId());
    }
}
