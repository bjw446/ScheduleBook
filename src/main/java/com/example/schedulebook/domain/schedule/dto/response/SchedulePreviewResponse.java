package com.example.schedulebook.domain.schedule.dto.response;

import com.example.schedulebook.domain.schedule.entity.Schedule;

import java.time.LocalDateTime;

public record SchedulePreviewResponse(
        Long messageId,
        Long scheduleId,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String owner,
        boolean deleted,
        boolean canceled,
        boolean edited
) {
    public static SchedulePreviewResponse from(
            Long messageId,
            Schedule schedule,
            boolean deleted,
            boolean canceled,
            boolean edited
    ) {
        return new SchedulePreviewResponse(
                messageId,
                schedule.getId(),
                schedule.getTitle(),
                LocalDateTime.from(schedule.getStartTime()),
                LocalDateTime.from(schedule.getEndTime()),
                schedule.getUser().getNickname(),
                deleted,
                canceled,
                edited
        );
    }
}
