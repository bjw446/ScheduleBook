package com.example.schedulebook.domain.schedulesnapshot.dto.response;

import com.example.schedulebook.domain.schedulesnapshot.entity.ScheduleSnapshot;

import java.time.LocalTime;

public record SchedulePreviewResponse(
        Long messageId,
        Long scheduleId,
        String title,
        LocalTime startTime,
        LocalTime endTime,
        boolean deleted,
        boolean canceled,
        boolean edited
) {
    public static SchedulePreviewResponse from(
            Long messageId,
            Long scheduleId,
            ScheduleSnapshot scheduleSnapshot,
            boolean deleted,
            boolean canceled,
            boolean edited
    ) {
        return new SchedulePreviewResponse(
                messageId,
                scheduleId,
                scheduleSnapshot.getTitle(),
                scheduleSnapshot.getStartTime(),
                scheduleSnapshot.getEndTime(),
                deleted,
                canceled,
                edited
        );
    }
}
