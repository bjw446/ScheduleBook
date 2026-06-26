package com.example.schedulebook.domain.schedule.dto.response;

public record ScheduleSnapshotFieldChangeResponse(
        String field,
        Object before,
        Object after
) {
}
