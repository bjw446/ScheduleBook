package com.example.schedulebook.domain.schedule_snapshot.entity;

import com.example.schedulebook.domain.schedule.entity.Schedule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleSnapshot {
    @Column(length = 50)
    private String title;

    @Column(length = 1000)
    private String content;

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    private Long scheduleVersion;

    @Column(name = "schedule_updated_at")
    private LocalDateTime scheduleUpdatedAt;

    public static ScheduleSnapshot from(Schedule schedule) {
        ScheduleSnapshot scheduleSnapshot = new ScheduleSnapshot();

        scheduleSnapshot.title = schedule.getTitle();
        scheduleSnapshot.content = schedule.getContent();
        scheduleSnapshot.scheduleDate = schedule.getScheduleDate();
        scheduleSnapshot.startTime = schedule.getStartTime();
        scheduleSnapshot.endTime = schedule.getEndTime();
        scheduleSnapshot.scheduleVersion = schedule.getScheduleVersion();
        scheduleSnapshot.scheduleUpdatedAt = schedule.getUpdatedAt();

        return scheduleSnapshot;
    }
}
