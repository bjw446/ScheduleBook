package com.example.schedulebook.domain.schedule.entity;

import com.example.schedulebook.common.entity.ModifyEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "schedule_reminders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleReminder extends ModifyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(nullable = false, name = "reminder_time")
    private LocalDateTime reminderTime;

    @Column(nullable = false)
    private boolean sent;

    public static ScheduleReminder of(Schedule schedule, LocalDateTime reminderTime) {
        ScheduleReminder scheduleReminder = new ScheduleReminder();

        scheduleReminder.schedule = schedule;
        scheduleReminder.reminderTime = reminderTime;
        scheduleReminder.sent = false;

        return scheduleReminder;
    }

    public void markSent() {
        this.sent = true;
    }
}
