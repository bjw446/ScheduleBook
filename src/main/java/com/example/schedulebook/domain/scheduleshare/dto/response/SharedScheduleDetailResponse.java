package com.example.schedulebook.domain.scheduleshare.dto.response;

import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;

import java.time.LocalDate;

public record SharedScheduleDetailResponse(
        Long shareId,
        Long scheduleId,
        String title,
        String contents,
        LocalDate scheduleDate,
        String ownerNickname
) {
    public static SharedScheduleDetailResponse from(ScheduleShare scheduleShare) {
        return new SharedScheduleDetailResponse(
                scheduleShare.getId(),
                scheduleShare.getSchedule().getId(),
                scheduleShare.getSchedule().getTitle(),
                scheduleShare.getSchedule().getContent(),
                scheduleShare.getSchedule().getScheduleDate(),
                scheduleShare.getSchedule().getUser().getNickname()
        );
    }
}
