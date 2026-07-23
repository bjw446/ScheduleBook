package com.example.schedulebook.domain.scheduleshare.listener;

import com.example.schedulebook.domain.schedule.event.ScheduleCanceledEvent;
import com.example.schedulebook.domain.schedule.event.ScheduleDeletedEvent;
import com.example.schedulebook.domain.schedule.event.ScheduleUpdatedEvent;
import com.example.schedulebook.domain.scheduleshare.service.ScheduleShareUpdateManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleShareEventListener {
    private final ScheduleShareUpdateManager scheduleShareUpdateManager;


    @EventListener
    public void handleScheduleUpdated(ScheduleUpdatedEvent event) {
        scheduleShareUpdateManager.handleUpdated(event.scheduleId());
    }

    @EventListener
    public void handleScheduleCanceled(ScheduleCanceledEvent event) {
        scheduleShareUpdateManager.handleCanceled(event.scheduleId(), event.sharedUserId());
    }

    @EventListener
    public void handleScheduleDeleted(ScheduleDeletedEvent event) {
        scheduleShareUpdateManager.handleDeleted(event.scheduleId());
    }
}
