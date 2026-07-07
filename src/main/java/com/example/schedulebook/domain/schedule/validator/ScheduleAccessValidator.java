package com.example.schedulebook.domain.schedule.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleAccessValidator {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleShareRepository scheduleShareRepository;

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
}
