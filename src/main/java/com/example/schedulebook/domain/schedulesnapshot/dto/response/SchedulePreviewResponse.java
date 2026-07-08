package com.example.schedulebook.domain.schedulesnapshot.dto.response;

import com.example.schedulebook.domain.schedulesnapshot.entity.ScheduleSnapshot;
import com.example.schedulebook.domain.schedulesnapshot.enums.SchedulePreviewState;

import java.time.LocalTime;

public record SchedulePreviewResponse(
        Long messageId,
        Long scheduleId,
        String title,
        LocalTime startTime,
        LocalTime endTime,
        SchedulePreviewState schedulePreviewState
) {
    public static SchedulePreviewResponse from(
            Long messageId,
            Long scheduleId,
            ScheduleSnapshot scheduleSnapshot,
            SchedulePreviewState schedulePreviewState
    ) {
        return new SchedulePreviewResponse(
                messageId,
                scheduleId,
                scheduleSnapshot.getTitle(),
                scheduleSnapshot.getStartTime(),
                scheduleSnapshot.getEndTime(),
                schedulePreviewState
        );
    }
}
