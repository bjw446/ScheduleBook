package com.example.schedulebook.domain.schedule.scheduler;

import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import com.example.schedulebook.domain.schedule.repository.ScheduleReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleReminderScheduler {
    private final ScheduleReminderRepository scheduleReminderRepository;

    @Scheduled(cron = "0 * * * * *")
    public void sendScheduleReminders() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        List<ScheduleReminder> reminders = scheduleReminderRepository.findByReminderTimeAndSent(now, false);

        for (ScheduleReminder reminder : reminders) {
            try {
                reminder.markSent();
                scheduleReminderRepository.save(reminder);

                log.info("알림 Reminder 발송 완료 scheduleId = {}", reminder.getSchedule().getId());

            } catch (Exception e) {
                log.error("일정 Reminder 발송 실패 scheduleId = {}", reminder.getSchedule().getId(), e);
            }
        }
    }
}
