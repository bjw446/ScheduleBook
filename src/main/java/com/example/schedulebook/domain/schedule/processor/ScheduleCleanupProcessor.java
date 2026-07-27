package com.example.schedulebook.domain.schedule.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.schedule.service.ScheduleService;
import com.example.schedulebook.domain.scheduleshare.service.ScheduleShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleCleanupProcessor {
    private final ScheduleShareService scheduleShareService;
    private final ScheduleService scheduleService;
    private final LoggingExecutor loggingExecutor;

    public boolean process(Long outboxId, Long userId) {
        boolean success = true;

        success &= loggingExecutor.execute(
                outboxId,
                "공유 받은 일정 및 공유 한 일정 삭제",
                () -> scheduleShareService.deleteAllShared(userId)
        );

        success &= loggingExecutor.execute(
                outboxId,
                "일정 참여자 및 일정 삭제",
                () -> scheduleService.deleteAllSchedules(userId)
        );

        return success;
    }
}
