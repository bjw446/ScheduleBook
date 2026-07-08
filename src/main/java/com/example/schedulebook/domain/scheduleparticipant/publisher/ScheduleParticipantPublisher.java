package com.example.schedulebook.domain.scheduleparticipant.publisher;

import com.example.schedulebook.common.websocket.WebSocketPublisher;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.scheduleparticipant.service.ScheduleParticipantReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.example.schedulebook.common.consts.WebSocketDestination.SCHEDULE_PARTICIPANTS;


@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleParticipantPublisher {
    private final ScheduleParticipantReader scheduleParticipantReader;
    private final WebSocketPublisher webSocketPublisher;

    public void publishParticipantsUpdated(Long scheduleId) {
        ScheduleParticipantListResponse response = scheduleParticipantReader.getParticipantList(scheduleId);

        webSocketPublisher.sendAfterCommit(
                SCHEDULE_PARTICIPANTS(scheduleId),
                response
        );
    }
}
