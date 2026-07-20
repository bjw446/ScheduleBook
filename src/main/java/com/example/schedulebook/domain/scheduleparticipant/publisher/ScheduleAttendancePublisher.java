package com.example.schedulebook.domain.scheduleparticipant.publisher;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.websocket.WebSocketPublisher;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleAttendanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleAttendancePublisher {
    private final WebSocketPublisher webSocketPublisher;

    public void publishAttendanceUpdated(ScheduleAttendanceResponse response) {
        webSocketPublisher.sendAfterCommit(
                WebSocketDestination.getScheduleDestination(response.scheduleId()),
                response
        );
    }
}
