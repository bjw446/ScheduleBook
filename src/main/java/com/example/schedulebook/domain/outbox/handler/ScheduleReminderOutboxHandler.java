package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.schedule.event.ScheduleReminderEvent;
import com.example.schedulebook.domain.schedule.processor.ScheduleReminderProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleReminderOutboxHandler implements OutboxEventHandler<ScheduleReminderEvent> {
    private final ScheduleReminderProcessor scheduleReminderProcessor;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.SCHEDULE_REMINDER;
    }

    @Override
    public Class<ScheduleReminderEvent> payloadType() {
        return ScheduleReminderEvent.class;
    }

    @Override
    public void handle(Long outboxId, ScheduleReminderEvent payload) {
        scheduleReminderProcessor.process(outboxId, payload);
    }
}
