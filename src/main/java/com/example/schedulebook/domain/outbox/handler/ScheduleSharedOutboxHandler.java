package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.scheduleshare.processor.ScheduleSharedProcessor;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.scheduleshare.event.ScheduleSharedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleSharedOutboxHandler implements OutboxEventHandler<ScheduleSharedEvent> {
    private final ScheduleSharedProcessor scheduleSharedProcessor;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.SCHEDULE_SHARED;
    }

    @Override
    public Class<ScheduleSharedEvent> payloadType() {
        return ScheduleSharedEvent.class;
    }

    @Override
    public void handle(ScheduleSharedEvent payload) {
        scheduleSharedProcessor.process(payload);
    }
}
