package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.notification.service.NotificationService;
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
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendScheduleReminders() {
        LocalDateTime now = LocalDateTime.now();

        List<Schedule> schedules = scheduleRepository.findSchedulesForReminder(
                now.toLocalDate(),
                now.toLocalTime().withSecond(0),
                now.toLocalTime().withSecond(0).plusMinutes(1)
        );

        for (Schedule schedule : schedules) {
            try {
                notificationService.createScheduleReminderNotification(
                        schedule.getUser().getId(),
                        schedule.getId(),
                        schedule.getTitle()
                );

                schedule.markReminderSent();

            } catch (Exception e) {
                log.error("일정 알림 발송 실패 scheduleId = {}", schedule.getId(), e);
            }
        }
    }
}
