package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.common.executor.AfterCommitExecutor;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleAttendanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleAttendancePublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AfterCommitExecutor afterCommitExecutor;

    public void publishAttendanceUpdated(ScheduleAttendanceResponse response) {
        afterCommitExecutor.execute(() -> {
            try {
                simpMessagingTemplate.convertAndSend(
                        "/topic/schedule/" + response.scheduleId(),
                        response
                );
            } catch (Exception e) {
                log.error("커밋 후 이벤트 발행 실패", e);
            }
        });
    }
}
