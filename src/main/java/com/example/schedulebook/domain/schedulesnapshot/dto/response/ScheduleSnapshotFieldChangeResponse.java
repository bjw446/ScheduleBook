package com.example.schedulebook.domain.schedulesnapshot.dto.response;

public record ScheduleSnapshotFieldChangeResponse(
        String field,
        Object before,
        Object after
) {
}
