package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleReminderProcessor {
    private final NotificationService notificationService;
    private final ScheduleRepository scheduleRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeReminderAndMarkSent(Schedule schedule) {
        notificationService.createScheduleReminderNotification(
                schedule.getUser(),
                schedule.getId(),
                schedule.getTitle()
        );

        schedule.markReminderSent();
        scheduleRepository.save(schedule);
    }
}
