package com.example.schedulebook.domain.scheduleparticipant.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleParticipantValidator {
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ScheduleShareRepository scheduleShareRepository;

    public void validateAlreadyParticipated(Long scheduleId, Long currentUserId) {
        if (scheduleParticipantRepository.existsBySchedule_IdAndUser_Id(scheduleId, currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_ALREADY_PARTICIPATED);
        }

        if (isAlreadyScheduleShared(scheduleId, currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_ALREADY_SHARED);
        }
    }

    public boolean isAlreadyScheduleShared(Long scheduleId, Long currentUserId) {
        return scheduleShareRepository.existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                scheduleId,
                currentUserId,
                ScheduleShareStatus.ACTIVE
        );
    }
}
