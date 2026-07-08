package com.example.schedulebook.domain.schedule.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class ScheduleValidator {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleShareRepository scheduleShareRepository;

    public Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND)
        );
    }

    public Schedule validateSchedule(Long scheduleId, Long currentUserId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND)
        );

        if (!schedule.getUser().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }

        return schedule;
    }

    public Schedule validateAccessibleSchedule(Long scheduleId, Long currentUserId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND)
        );

        if (schedule.getUser().getId().equals(currentUserId)) {
            return schedule;
        }

        scheduleShareRepository.findActiveRelation(scheduleId, currentUserId, ScheduleShareStatus.ACTIVE).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN)
        );

        return schedule;
    }

    public void validateYearMonth(int year, int month) {
        try {
            YearMonth yearMonth = YearMonth.of(year, month);

        } catch (DateTimeException e) {
            throw new BaseException(ErrorEnum.INVALID_SCHEDULE_MONTH);
        }
    }

    public void validateScheduleOwner(Schedule schedule, Long currentUserId) {
        if (!schedule.getUser().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }
    }
}
