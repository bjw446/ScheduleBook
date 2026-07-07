package com.example.schedulebook.domain.schedule_snapshot.dto.response;

public record ScheduleSnapshotFieldChangeResponse(
        String field,
        Object before,
        Object after
) {
}
