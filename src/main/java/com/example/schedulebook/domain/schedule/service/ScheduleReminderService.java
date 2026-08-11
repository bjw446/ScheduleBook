package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus;
import com.example.schedulebook.domain.schedule.event.ScheduleReminderEvent;
import com.example.schedulebook.domain.schedule.repository.ScheduleReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

        outboxService.cancelPending(
                OutboxAggregateType.SCHEDULE,
                String.valueOf(schedule.getId()),
                OutboxEventType.SCHEDULE_REMINDER
        );

        save(schedule);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processReminderSent(Long scheduleReminderId, String claimToken) {
        ScheduleReminder scheduleReminder = scheduleReminderRepository.findById(scheduleReminderId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_REMINDER_NOT_FOUND)
        );

        if (!claimToken.equals(scheduleReminder.getClaimToken())
                || scheduleReminder.getScheduleReminderStatus()
                != ScheduleReminderStatus.PROCESSING) {
            return;
        }

        ScheduleReminderEvent reminderEvent = new ScheduleReminderEvent(
                scheduleReminder.getSchedule().getId(),
                scheduleReminder.getSchedule().getUser().getId(),
                scheduleReminder.getSchedule().getTitle(),
                scheduleReminder.getReminderTime()
        );

        outboxService.save(
                OutboxAggregateType.SCHEDULE,
                String.valueOf(scheduleReminder.getSchedule().getId()),
                OutboxEventType.SCHEDULE_REMINDER,
                reminderEvent
        );

        boolean sent = markSent(scheduleReminderId, claimToken);

        if (!sent) {
            throw new BaseException(ErrorEnum.SCHEDULE_REMINDER_STATUS_CHANGE_FAILED);
        }
    }

    @Transactional
    public boolean markProcessing(Long scheduleReminderId, String claimToken, LocalDateTime claimedAt) {
        return scheduleReminderRepository.markProcessing(scheduleReminderId, claimToken, claimedAt) == 1;
    }

    @Transactional
    public boolean markSent(Long scheduleReminderId, String claimToken) {
        return scheduleReminderRepository.markSent(scheduleReminderId, claimToken) == 1;
    }

    @Transactional
    public boolean markPending(Long scheduleReminderId, String claimToken) {
        return scheduleReminderRepository.markPending(scheduleReminderId, claimToken) == 1;
    }

    @Transactional
    public int recoverStuckReminders(LocalDateTime threshold) {
        return scheduleReminderRepository.recoverStuckReminders(threshold);
    }

    @Transactional(readOnly = true)
    public List<ScheduleReminder> findPendingReminders(LocalDateTime now, Pageable pageable) {
        return scheduleReminderRepository.findReminders(now, pageable);
    }

    @Transactional(readOnly = true)
    public long countPendingReminders() {
        return scheduleReminderRepository.countByScheduleReminderStatus(ScheduleReminderStatus.PENDING);
    }
}
