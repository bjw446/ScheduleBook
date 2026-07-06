package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.common.websocket.WebSocketPublisher;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleAttendanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.example.schedulebook.common.consts.WebSocketDestination.schedule;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleAttendancePublisher {
    private final WebSocketPublisher webSocketPublisher;

    public void publishAttendanceUpdated(ScheduleAttendanceResponse response) {
        webSocketPublisher.sendAfterCommit(
                schedule(response.scheduleId()),
                response
        );
    }
}
