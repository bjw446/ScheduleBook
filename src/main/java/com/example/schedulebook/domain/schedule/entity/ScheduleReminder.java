package com.example.schedulebook.domain.schedule.entity;

import com.example.schedulebook.common.entity.ModifyEntity;
import com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "schedule_reminders",
        indexes = @Index(
                name = "idx_schedule_reminders_status_time_id",
                columnList = "schedule_reminder_status, reminder_time, id"
        )
)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_reminder_status", nullable = false)
    private ScheduleReminderStatus scheduleReminderStatus;

    @Column(name = "claim_token")
    private String claimToken;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    public static ScheduleReminder of(Schedule schedule, LocalDateTime reminderTime) {
        ScheduleReminder scheduleReminder = new ScheduleReminder();

        scheduleReminder.schedule = schedule;
        scheduleReminder.reminderTime = reminderTime;
        scheduleReminder.scheduleReminderStatus = ScheduleReminderStatus.PENDING;

        return scheduleReminder;
    }
}
