package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.schedule.dto.request.CreateScheduleRequest;
import com.example.schedulebook.domain.schedule.dto.request.UpdateScheduleRequest;
import com.example.schedulebook.domain.schedule.event.ScheduleDeletedEvent;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleDetailResponse;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.example.schedulebook.domain.schedule.event.ScheduleUpdatedEvent;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ScheduleSummaryResponse createSchedule(CreateScheduleRequest request, Long currentUserId) {
        User user = validateUser(currentUserId);

        Schedule schedule = Schedule.create(
                user,
                request.title(),
                request.content(),
                request.scheduleDate(),
                request.startTime(),
                request.endTime()
        );

        Schedule savedSchedule = scheduleRepository.save(schedule);

        user.increaseScheduleCount();

        return ScheduleSummaryResponse.from(savedSchedule);
    }

    @Transactional(readOnly = true)
    public ScheduleDetailResponse findOneSchedule(Long scheduleId, Long currentUserId) {
        validateUser(currentUserId);

        Schedule schedule = validateSchedule(scheduleId, currentUserId);

        return ScheduleDetailResponse.from(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleSummaryResponse> findSchedulesByMonth(int year, int month, Long currentUserId) {
        validateUser(currentUserId);

        validateYearMonth(year, month);

        LocalDate start = LocalDate.of(year, month, 1);

        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Schedule> schedules = scheduleRepository.findAllByUserIdAndScheduleDateBetween(currentUserId, start, end);

        return schedules.stream()
                .map(ScheduleSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleSummaryResponse> findSchedulesByDate(LocalDate date, Long currentUserId) {
        validateUser(currentUserId);

        List<Schedule> schedules = scheduleRepository.findByUser_IdAndScheduleDate(currentUserId, date);

        return schedules.stream()
                .map(ScheduleSummaryResponse::from)
                .toList();
    }

    public ScheduleSummaryResponse updateSchedule(Long scheduleId, UpdateScheduleRequest request, Long currentUserId) {
        validateUser(currentUserId);

        Schedule schedule = validateSchedule(scheduleId, currentUserId);

        schedule.update(
                request.title(),
                request.content(),
                request.scheduleDate(),
                request.startTime(),
                request.endTime()
        );

        applicationEventPublisher.publishEvent(new ScheduleUpdatedEvent(schedule.getId()));

        return ScheduleSummaryResponse.from(schedule);
    }

    public void deleteSchedule(Long scheduleId, Long currentUserId) {
        validateUser(currentUserId);

        Schedule schedule = validateSchedule(scheduleId, currentUserId);

        schedule.delete();

        applicationEventPublisher.publishEvent(new ScheduleDeletedEvent(schedule.getId()));
    }

    private User validateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.USER_NOT_ACTIVE);
        }

        return user;
    }

    private Schedule validateSchedule(Long scheduleId, Long currentUserId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND)
        );

        if (!schedule.getUser().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }

        return schedule;
    }

    private void validateYearMonth(int year, int month) {
        try {
            YearMonth yearMonth = YearMonth.of(year, month);

        } catch (DateTimeException e) {
            throw new BaseException(ErrorEnum.INVALID_SCHEDULE_MONTH);
        }
    }
}
