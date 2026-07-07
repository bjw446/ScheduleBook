package com.example.schedulebook.domain.schedule_snapshot.dto.response;

import java.util.List;

public record ScheduleSnapshotDiffResponse(
        Long fromVersion,
        Long toVersion,
        List<ScheduleSnapshotFieldChangeResponse> changes
) {
}
