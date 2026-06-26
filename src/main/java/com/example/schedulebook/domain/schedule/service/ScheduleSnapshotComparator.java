package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.schedule.dto.response.ScheduleSnapshotDiffResponse;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleSnapshotFieldChangeResponse;
import com.example.schedulebook.domain.schedule.entity.ScheduleSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ScheduleSnapshotComparator {

    public ScheduleSnapshotDiffResponse compare(ScheduleSnapshot before, ScheduleSnapshot after) {
        List<ScheduleSnapshotFieldChangeResponse> changes = new ArrayList<>();

        compareField(changes, "title", before.getTitle(), after.getTitle());

        compareField(changes, "content", before.getContent(), after.getContent());

        compareField(changes, "scheduleDate", before.getScheduleDate(), after.getScheduleDate());

        compareField(changes, "startTime", before.getStartTime(), after.getStartTime());

        compareField(changes, "endTime", before.getEndTime(), after.getEndTime());

        return new ScheduleSnapshotDiffResponse(before.getScheduleVersion(), after.getScheduleVersion(), changes);
    }

    private void compareField(List<ScheduleSnapshotFieldChangeResponse> changes, String field, Object before, Object after) {
        if (Objects.equals(before, after)) {
            return;
        }

        changes.add(new ScheduleSnapshotFieldChangeResponse(field, before, after));
    }
}
