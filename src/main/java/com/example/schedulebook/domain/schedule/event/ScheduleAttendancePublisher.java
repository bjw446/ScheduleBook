package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.domain.schedule.dto.response.ScheduleAttendanceResponse;
import com.example.schedulebook.domain.schedule.enums.AttendanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class ScheduleAttendancePublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void publishAttendanceUpdated(ScheduleAttendanceResponse response) {
        afterCommit(() -> simpMessagingTemplate.convertAndSend(
                "/topic/schedule/" + response.scheduleId(),
                response
        ));
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
