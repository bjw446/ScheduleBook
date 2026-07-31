package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import com.example.schedulebook.domain.schedule.repository.ScheduleReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleReminderService {
    private final ScheduleReminderRepository scheduleReminderRepository;
    private final OutboxService outboxService;

    @Transactional
    public void save(Schedule schedule) {
        LocalDateTime reminderTime = LocalDateTime.of(schedule.getScheduleDate(), schedule.getStartTime());

        LocalDateTime reminderBeforeTenMin = reminderTime.minusMinutes(10);

        LocalDateTime reminderBeforeDay = reminderTime.minusDays(1);

        ScheduleReminder scheduleReminder = ScheduleReminder.of(schedule, reminderTime);

        ScheduleReminder scheduleReminderBeforeTenMin = ScheduleReminder.of(schedule, reminderBeforeTenMin);

        ScheduleReminder scheduleReminderBeforeDay = ScheduleReminder.of(schedule, reminderBeforeDay);

        scheduleReminderRepository.save(scheduleReminder);

        scheduleReminderRepository.save(scheduleReminderBeforeTenMin);

        scheduleReminderRepository.save(scheduleReminderBeforeDay);
    }

    @Transactional
    public void refresh(Schedule schedule) {
        scheduleReminderRepository.deleteBySchedule_Id(schedule.getId());

        outboxService.cancelPending(OutboxAggregateType.SCHEDULE, schedule.getId(), OutboxEventType.SCHEDULE_REMINDER);

        save(schedule);
    }
}
