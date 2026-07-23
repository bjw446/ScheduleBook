package com.example.schedulebook.domain.schedule.listener;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.schedule.service.ScheduleService;
import com.example.schedulebook.domain.scheduleshare.service.ScheduleShareService;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ScheduleCleanupListener {
    private final ScheduleShareService scheduleShareService;
    private final ScheduleService scheduleService;
    private final LoggingExecutor loggingExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserWithdrawEvent event) {
        loggingExecutor.execute("공유 받은 일정 및 공유 한 일정 삭제", () -> scheduleShareService.deleteAllShared(event.userId()));

        loggingExecutor.execute("일정 참여자 및 일정 삭제", () -> scheduleService.deleteAllSchedules(event.userId()));
    }
}
