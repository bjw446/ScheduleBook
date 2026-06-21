package com.example.schedulebook.domain.schedule.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String title;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false, name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(nullable = false, name = "start_time")
    private LocalTime startTime;

    @Column(nullable = false, name = "end_time")
    private LocalTime endTime;

    @Column(nullable = false, name = "start_time_specified")
    private boolean startTimeSpecified;

    @Column(nullable = false, name = "end_time_specified")
    private boolean endTimeSpecified;

    public static Schedule create(
            User user,
            String title,
            String content,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        Schedule schedule = new Schedule();

        schedule.user = user;
        schedule.title = title;
        schedule.content = content;
        schedule.scheduleDate = scheduleDate;

        schedule.applyTime(startTime, endTime);

        return schedule;
    }

    public void update(
            String title,
            String content,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.title = title;
        this.content = content;
        this.scheduleDate = scheduleDate;

        applyTime(startTime, endTime);
    }

    private void validateTime() {
        if (!startTime.isBefore(endTime)) {
            throw new BaseException(ErrorEnum.INVALID_SCHEDULE_TIME);
        }
    }

    private void applyTime(LocalTime startTime, LocalTime endTime) {
        this.startTimeSpecified = startTime != null;
        this.endTimeSpecified = endTime != null;

        this.startTime = startTime != null ? startTime : LocalTime.MIN;

        this.endTime = endTime != null ? endTime : LocalTime.MAX;

        validateTime();
    }
}