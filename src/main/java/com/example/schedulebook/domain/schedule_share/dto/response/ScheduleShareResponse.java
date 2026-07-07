package com.example.schedulebook.domain.schedule_share.dto.response;

import com.example.schedulebook.domain.schedule_share.entity.ScheduleShare;

public record ScheduleShareResponse(
        Long shareId,
        Long scheduleId,
        Long sharedUserId,
        String sharedUserNickname
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
