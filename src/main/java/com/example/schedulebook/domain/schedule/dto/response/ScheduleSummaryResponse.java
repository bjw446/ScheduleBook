package com.example.schedulebook.domain.schedule.dto.response;

import com.example.schedulebook.domain.schedule.entity.Schedule;

import java.time.LocalDate;

public record ScheduleSummaryResponse(
        Long scheduleId,
        String title,
        int commentCount,
        LocalDate scheduleDate
) {
    public static ScheduleSummaryResponse from(Schedule schedule) {
        return new ScheduleSummaryResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getCommentCount(),
                schedule.getScheduleDate()
        );
    }
}
