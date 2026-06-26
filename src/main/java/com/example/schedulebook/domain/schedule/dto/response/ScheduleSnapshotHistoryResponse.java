package com.example.schedulebook.domain.schedule.dto.response;


import com.example.schedulebook.domain.schedule.entity.ScheduleSnapshotHistory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduleSnapshotHistoryResponse(
        String title,
        String content,
        LocalDate scheduleDate,
        LocalTime startTime,
        LocalTime endTime,
        Long scheduleVersion,
        LocalDateTime scheduleUpdatedAt
) {
    public static ScheduleSnapshotHistoryResponse from(ScheduleSnapshotHistory scheduleSnapshotHistory) {
        return new ScheduleSnapshotHistoryResponse(
                scheduleSnapshotHistory.getScheduleSnapshot().getTitle(),
                scheduleSnapshotHistory.getScheduleSnapshot().getContent(),
                scheduleSnapshotHistory.getScheduleSnapshot().getScheduleDate(),
                scheduleSnapshotHistory.getScheduleSnapshot().getStartTime(),
                scheduleSnapshotHistory.getScheduleSnapshot().getEndTime(),
                scheduleSnapshotHistory.getScheduleSnapshot().getScheduleVersion(),
                scheduleSnapshotHistory.getScheduleSnapshot().getScheduleUpdatedAt()
        );
    }
}
