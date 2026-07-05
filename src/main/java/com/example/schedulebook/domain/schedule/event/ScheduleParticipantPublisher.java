package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.common.websocket.WebSocketPublisher;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.schedule.service.ScheduleParticipantReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleParticipantPublisher {
    private final ScheduleParticipantReader scheduleParticipantReader;
    private final WebSocketPublisher webSocketPublisher;

    public void publishParticipantsUpdated(Long scheduleId) {
        ScheduleParticipantListResponse response = scheduleParticipantReader.getParticipantList(scheduleId);

        webSocketPublisher.sendAfterCommit(
                "/topic/schedule/" + scheduleId + "/participants",
                response
        );
    }
}
