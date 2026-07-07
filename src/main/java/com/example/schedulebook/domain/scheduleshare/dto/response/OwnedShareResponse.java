package com.example.schedulebook.domain.scheduleshare.dto.response;

import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;

public record OwnedShareResponse(
        Long shareId,
        Long scheduleId,
        String title,
        Long sharedUserId,
        String sharedUserNickname
) {
    public static OwnedShareResponse from(ScheduleShare scheduleShare) {
        return new OwnedShareResponse(
                scheduleShare.getId(),
                scheduleShare.getSchedule().getId(),
                scheduleShare.getSchedule().getTitle(),
                scheduleShare.getSharedUser().getId(),
                scheduleShare.getSharedUser().getNickname()
        );
    }
}
