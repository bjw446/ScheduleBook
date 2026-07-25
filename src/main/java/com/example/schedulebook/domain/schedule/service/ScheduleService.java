package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.event.OutboxSaveEvent;
import com.example.schedulebook.domain.outbox.service.OutboxPublishService;
import com.example.schedulebook.domain.schedule.dto.request.CreateScheduleRequest;
import com.example.schedulebook.domain.schedule.dto.request.UpdateScheduleRequest;
import com.example.schedulebook.domain.schedule.validator.ScheduleValidator;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantInfo;
import com.example.schedulebook.domain.scheduleparticipant.service.ScheduleParticipantReader;
import com.example.schedulebook.domain.schedule.event.ScheduleDeletedEvent;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleDetailResponse;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.example.schedulebook.domain.schedule.event.ScheduleUpdatedEvent;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleparticipant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ScheduleParticipantReader scheduleParticipantReader;
    private final UserValidator userValidator;
    private final ScheduleValidator scheduleValidator;
    private final OutboxPublishService outboxPublishService;

    public ScheduleSummaryResponse createSchedule(CreateScheduleRequest request, Long currentUserId) {
        User user = userValidator.validateActiveUser(currentUserId);

        Schedule schedule = Schedule.create(
                user,
                request.title(),
                request.content(),
                request.scheduleDate(),
                request.startTime(),
                request.endTime()
        );

        Schedule savedSchedule = scheduleRepository.save(schedule);

        ScheduleParticipant ownerParticipant = ScheduleParticipant.of(savedSchedule, user);

        scheduleParticipantRepository.save(ownerParticipant);

        user.increaseScheduleCount();

        return ScheduleSummaryResponse.from(savedSchedule);
    }

    @Transactional(readOnly = true)
    public ScheduleDetailResponse findOneSchedule(Long scheduleId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        Schedule schedule = scheduleValidator.validateSchedule(scheduleId, currentUserId);

        ScheduleParticipantInfo info = scheduleParticipantReader.getParticipantInfo(schedule.getId(), currentUserId);

        return ScheduleDetailResponse.from(schedule, info);
    }

    @Transactional(readOnly = true)
    public List<ScheduleSummaryResponse> findSchedulesByMonth(int year, int month, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        scheduleValidator.validateYearMonth(year, month);

        LocalDate startDate = LocalDate.of(year, month, 1);

        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Schedule> schedules = scheduleRepository.findAllByUserIdAndScheduleDateBetween(currentUserId, startDate, endDate);

        return schedules.stream()
                .map(ScheduleSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleSummaryResponse> findSchedulesByDate(LocalDate date, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        List<Schedule> schedules = scheduleRepository.findByUser_IdAndScheduleDate(currentUserId, date);

        return schedules.stream()
                .map(ScheduleSummaryResponse::from)
                .toList();
    }

    public ScheduleSummaryResponse updateSchedule(Long scheduleId, UpdateScheduleRequest request, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        Schedule schedule = scheduleValidator.validateSchedule(scheduleId, currentUserId);

        schedule.update(
                request.title(),
                request.content(),
                request.scheduleDate(),
                request.startTime(),
                request.endTime()
        );

        ScheduleUpdatedEvent scheduleUpdatedEvent = new ScheduleUpdatedEvent(schedule.getId());

        outboxPublishService.publish(new OutboxSaveEvent(
                OutboxAggregateType.SCHEDULE,
                schedule.getId(),
                OutboxEventType.SCHEDULE_UPDATED,
                scheduleUpdatedEvent
        ));

        return ScheduleSummaryResponse.from(schedule);
    }

    public void deleteSchedule(Long scheduleId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        Schedule schedule = scheduleValidator.validateSchedule(scheduleId, currentUserId);

        schedule.delete();

        ScheduleDeletedEvent scheduleDeletedEvent = new ScheduleDeletedEvent(schedule.getId());

        outboxPublishService.publish(new OutboxSaveEvent(
                OutboxAggregateType.SCHEDULE,
                schedule.getId(),
                OutboxEventType.SCHEDULE_DELETED,
                scheduleDeletedEvent
        ));
    }

    public void deleteAllSchedules(Long userId) {
        scheduleParticipantRepository.deleteAllByUserId(userId);

        scheduleParticipantRepository.deleteByOwnedSchedule(userId);

        scheduleRepository.softDeleteAllByUserId(userId);
    }
}
