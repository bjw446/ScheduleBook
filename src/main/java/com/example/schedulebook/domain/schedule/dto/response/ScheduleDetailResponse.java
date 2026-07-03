package com.example.schedulebook.domain.schedule.dto.response;

import com.example.schedulebook.domain.schedule.entity.Schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleDetailResponse(
        Long scheduleId,
        String title,
        String content,
        int commentCount,
        LocalDate scheduleDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean startTimeSpecified,
        boolean endTimeSpecified,
        boolean participated,
        int participantCount,
        List<ScheduleParticipantResponse> participants
) {
    public static ScheduleDetailResponse from(
            Schedule schedule,
            boolean participated,
            int participantCount,
            List<ScheduleParticipantResponse> participants
    ) {
        return new ScheduleDetailResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getContent(),
                schedule.getCommentCount(),
                schedule.getScheduleDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isStartTimeSpecified(),
                schedule.isEndTimeSpecified(),
                participated,
                participantCount,
                participants
        );
    }
}
