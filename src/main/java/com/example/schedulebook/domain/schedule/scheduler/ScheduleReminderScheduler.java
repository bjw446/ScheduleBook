package com.example.schedulebook.domain.schedule.scheduler;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.metrics.RetrySchedulerMetrics;
import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus;
import com.example.schedulebook.domain.schedule.service.ScheduleReminderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleReminderScheduler {
    private final ScheduleReminderService scheduleReminderService;
    private final RetrySchedulerMetrics retrySchedulerMetrics;
    private static final String METRIC = "schedule_reminder";
    private static final String RECOVERY_METRIC = "schedule_reminder_recovery";

    @Scheduled(cron = "0 * * * * *")
    public void sendScheduleReminders() {
        retrySchedulerMetrics.schedulerRun(METRIC);

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        for (int batch = 0; batch < CommonConst.MAX_BATCHES_PER_RUN; batch++) {
            List<ScheduleReminder> reminders = scheduleReminderService.findPendingReminders(
                    now,
                    PageRequest.of(0, CommonConst.BATCH_SIZE)
            );

            if (reminders.isEmpty()) {
                break;
            }

            for (ScheduleReminder reminder : reminders) {
                String claimToken = UUID.randomUUID().toString();

                boolean claimed = scheduleReminderService.markProcessing(reminder.getId(), claimToken, LocalDateTime.now());

                if (!claimed) {
                    continue;
                }

                retrySchedulerMetrics.processed(METRIC);

                try {
                    if (scheduleReminderService.processReminderSent(reminder.getId(), claimToken)) {
                        retrySchedulerMetrics.success(METRIC);

                        log.debug("알림 Outbox 이벤트 생성 완료 scheduleId = {}, reminderTime = {}",
                                reminder.getSchedule().getId(),
                                reminder.getReminderTime()
                        );
                    } else {
                        log.warn("알림 Outbox 이벤트 생성 건너뜀 scheduleId = {}, reminderTime = {}",
                                reminder.getSchedule().getId(),
                                reminder.getReminderTime());
                    }

                } catch (Exception e) {
                    retrySchedulerMetrics.error(METRIC);

                    log.error("일정 Outbox 이벤트 생성 실패 scheduleId = {}, reminderTime = {}",
                            reminder.getSchedule().getId(),
                            reminder.getReminderTime(),
                            e
                    );

                    boolean pending = scheduleReminderService.markPending(reminder.getId(), claimToken);

                    if (!pending) {
                        retrySchedulerMetrics.error(METRIC);

                        log.warn("ScheduleReminder PENDING 복구 실패 scheduleReminderId = {}",
                                reminder.getId()
                        );
                    }
                }
            }
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void recoverStuckReminders() {
        retrySchedulerMetrics.schedulerRun(RECOVERY_METRIC);

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CommonConst.SCHEDULE_REMINDER_PROCESSING_TIMEOUT_MINUTES);

        int recovered = scheduleReminderService.recoverStuckReminders(threshold);

        if (recovered > 0) {
            retrySchedulerMetrics.recovered(RECOVERY_METRIC, recovered);

            log.warn("오래된 ScheduleReminder {}건을 PENDING으로 복구했습니다.", recovered);
        }
    }

    @PostConstruct
    public void registerMetrics() {
        retrySchedulerMetrics.registerPendingGauge(
                METRIC,
                scheduleReminderService::countPendingReminders
        );
    }
}
