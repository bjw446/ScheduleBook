package com.example.schedulebook.domain.scheduleshare.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleShareValidator {
    private final ScheduleShareRepository scheduleShareRepository;

    public ScheduleShare validateActiveScheduleShare(Long shareId) {
        ScheduleShare scheduleShare = scheduleShareRepository.findByIdWithSchedule(shareId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_SHARE_NOT_FOUND)
        );

        if (scheduleShare.getScheduleShareStatus() != ScheduleShareStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.INVALID_SCHEDULE_SHARE_STATUS);
        }

        return scheduleShare;
    }

    public void validateShareStatus(ScheduleShare scheduleShare) {
        if (scheduleShare.getScheduleShareStatus() == ScheduleShareStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.SCHEDULE_ALREADY_SHARED);
        }
    }

    public ScheduleShare validateScheduleShare(Long shareId) {
        return scheduleShareRepository.findActiveShareDetail(shareId, ScheduleShareStatus.ACTIVE).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_SHARE_NOT_FOUND)
        );
    }

    public void validateSharedUser(ScheduleShare scheduleShare, Long currentUserId) {
        if (!scheduleShare.getSharedUser().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }
    }

    public void validateShareOwner(ScheduleShare scheduleShare, Long currentUserId) {
        if (!scheduleShare.getSchedule().getUser().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }
    }

    public ScheduleShare validateOwnedShare(Long shareId) {
        return scheduleShareRepository.findOwnedShareDetail(shareId, ScheduleShareStatus.ACTIVE).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_SHARE_NOT_FOUND)
        );
    }
}
