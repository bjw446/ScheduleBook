package com.example.schedulebook.domain.schedulesnapshot.dto.response;

import java.util.List;

public record ScheduleSnapshotDiffResponse(
        Long fromVersion,
        Long toVersion,
        List<ScheduleSnapshotFieldChangeResponse> changes
) {
}
