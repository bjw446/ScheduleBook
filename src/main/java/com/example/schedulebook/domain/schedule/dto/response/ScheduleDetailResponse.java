package com.example.schedulebook.domain.schedule.dto.response;

import com.example.schedulebook.domain.schedule.entity.Schedule;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleDetailResponse(
        Long scheduleId,
        String title,
        String content,
        LocalDate scheduleDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean startTimeSpecified,
        boolean endTimeSpecified
) {
    public static ScheduleDetailResponse from(Schedule schedule) {
        return new ScheduleDetailResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getContent(),
                schedule.getScheduleDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isStartTimeSpecified(),
                schedule.isEndTimeSpecified()
        );
    }
}
