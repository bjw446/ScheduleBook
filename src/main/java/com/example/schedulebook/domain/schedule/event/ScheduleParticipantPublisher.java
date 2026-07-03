package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.domain.schedule.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.schedule.service.ScheduleParticipantReader;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


@Component
@RequiredArgsConstructor
public class ScheduleParticipantPublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ScheduleParticipantReader scheduleParticipantReader;

    public void publishParticipantsUpdated(Long scheduleId) {
        ScheduleParticipantListResponse response = scheduleParticipantReader.getParticipantList(scheduleId);

        afterCommit(() -> simpMessagingTemplate.convertAndSend(
                "/topic/schedule/" + scheduleId + "/participants",
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
