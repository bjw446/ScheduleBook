package com.example.schedulebook.domain.scheduleshare.dto.response;

import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;

public record ScheduleShareResponse(
        Long shareId,
        Long scheduleId,
        Long sharedUserId,
        String SharedUserNickname
) {
    public static ScheduleShareResponse from(ScheduleShare scheduleShare) {
        return new ScheduleShareResponse(
                scheduleShare.getId(),
                scheduleShare.getSchedule().getId(),
                scheduleShare.getSharedUser().getId(),
                scheduleShare.getSharedUser().getNickname()
        );
    }
}
