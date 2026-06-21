package com.example.schedulebook.domain.scheduleshare.dto.response;

import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;

import java.time.LocalDate;

public record OwnedShareDetailResponse(
        Long shareId,
        Long scheduleId,
        String title,
        String content,
        LocalDate scheduleDate,
        Long sharedUserId,
        String sharedUserNickname
) {
    public static OwnedShareDetailResponse from(ScheduleShare scheduleShare) {
        return new OwnedShareDetailResponse(
                scheduleShare.getId(),
                scheduleShare.getSchedule().getId(),
                scheduleShare.getSchedule().getTitle(),
                scheduleShare.getSchedule().getContent(),
                scheduleShare.getSchedule().getScheduleDate(),
                scheduleShare.getSharedUser().getId(),
                scheduleShare.getSharedUser().getNickname()
        );
    }
}
