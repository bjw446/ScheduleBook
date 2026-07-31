package com.example.schedulebook.domain.schedule.scheduler;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import com.example.schedulebook.domain.schedule.event.ScheduleReminderEvent;
import com.example.schedulebook.domain.schedule.repository.ScheduleReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleReminderScheduler {
    private final ScheduleReminderRepository scheduleReminderRepository;
    private final OutboxService outboxService;

    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void sendScheduleReminders() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        for (int batch = 0; batch < CommonConst.MAX_BATCHES_PER_RUN; batch++) {
            List<ScheduleReminder> reminders = scheduleReminderRepository.findReminders(
                    now,
                    PageRequest.of(0, CommonConst.BATCH_SIZE, Sort.by("reminderTime").ascending())
            );

            if (reminders.isEmpty()) {
                break;
            }

            for (ScheduleReminder reminder : reminders) {
                try {
                    ScheduleReminderEvent reminderEvent = new ScheduleReminderEvent(
                            reminder.getSchedule().getId(),
                            reminder.getSchedule().getUser().getId(),
                            reminder.getSchedule().getTitle(),
                            reminder.getReminderTime()
                    );

                    outboxService.save(
                            OutboxAggregateType.SCHEDULE,
                            reminder.getSchedule().getId(),
                            OutboxEventType.SCHEDULE_REMINDER,
                            reminderEvent
                    );

                    reminder.markSent();

                    log.debug("알림 Outbox 이벤트 생성 완료 scheduleId = {}, reminderTime = {}",
                            reminder.getSchedule().getId(),
                            reminder.getReminderTime()
                    );

                } catch (Exception e) {
                    log.error("일정 Outbox 이벤트 생성 실패 scheduleId = {}, reminderTime = {}",
                            reminder.getSchedule().getId(),
                            reminder.getReminderTime(),
                            e
                    );
                }
            }

            scheduleReminderRepository.saveAll(reminders);
        }
    }
}
