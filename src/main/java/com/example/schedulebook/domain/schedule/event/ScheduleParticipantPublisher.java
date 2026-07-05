package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.common.executor.AfterCommitExecutor;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.schedule.service.ScheduleParticipantReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleParticipantPublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ScheduleParticipantReader scheduleParticipantReader;
    private final AfterCommitExecutor afterCommitExecutor;

    public void publishParticipantsUpdated(Long scheduleId) {
        ScheduleParticipantListResponse response = scheduleParticipantReader.getParticipantList(scheduleId);

        afterCommitExecutor.execute(() -> {
            try {
                simpMessagingTemplate.convertAndSend(
                        "/topic/schedule/" + scheduleId + "/participants",
                        response
                );
            } catch (Exception e) {
                log.error("참가자 업데이트 웹소켓 전송 실패", e);
            }
        });
    }
}
