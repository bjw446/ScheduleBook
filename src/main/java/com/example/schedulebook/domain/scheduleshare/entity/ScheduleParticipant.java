package com.example.schedulebook.domain.scheduleshare.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.scheduleshare.enums.AttendanceStatus;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "schedule_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "schedule_id",
                                "user_id"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleParticipant extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false)
    private AttendanceStatus attendanceStatus;

    public static ScheduleParticipant of(Schedule schedule, User user) {
        ScheduleParticipant scheduleParticipant = new ScheduleParticipant();

        scheduleParticipant.schedule = schedule;
        scheduleParticipant.user = user;
        scheduleParticipant.attendanceStatus = AttendanceStatus.PENDING;

        return scheduleParticipant;
    }

    public void updateAttendanceStatus(AttendanceStatus attendanceStatus) {
        if (attendanceStatus == null) {
            throw new BaseException(ErrorEnum.INVALID_SCHEDULE_ATTENDANCE_STATUS);
        }
        this.attendanceStatus = attendanceStatus;
    }
}
