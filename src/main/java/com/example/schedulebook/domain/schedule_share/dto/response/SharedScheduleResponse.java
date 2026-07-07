package com.example.schedulebook.domain.schedule_share.dto.response;

import com.example.schedulebook.domain.schedule_share.entity.ScheduleShare;

import java.time.LocalDate;

public record SharedScheduleResponse(
        Long shareId,
        Long scheduleId,
        String title,
        LocalDate scheduleDate,
        String ownerNickname
) {
    public static SharedScheduleResponse from(ScheduleShare scheduleShare) {
        return new SharedScheduleResponse(
                scheduleShare.getId(),
                scheduleShare.getSchedule().getId(),
                scheduleShare.getSchedule().getTitle(),
                scheduleShare.getSchedule().getScheduleDate(),
                scheduleShare.getSchedule().getUser().getNickname()
        );
    }
}
