package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleReminderService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleReminderProcessor scheduleReminderProcessor;

    @Scheduled(cron = "0 * * * * *")
    public void sendScheduleReminders() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        List<Schedule> schedules = scheduleRepository.findSchedulesForReminder(
                now.toLocalDate(),
                now.toLocalTime()
        );

        for (Schedule schedule : schedules) {
            try {
                scheduleReminderProcessor.executeReminderAndMarkSent(schedule);

            } catch (Exception e) {
                log.error("일정 알림 발송 실패 scheduleId = {}", schedule.getId(), e);
            }
        }
    }
}
