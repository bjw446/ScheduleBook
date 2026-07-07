package com.example.schedulebook.domain.schedule_share.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule_share.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "schedule_shares",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "schedule_id",
                                "shared_user_id"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleShare extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_user_id", nullable = false)
    private User sharedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleShareStatus scheduleShareStatus;

    public static ScheduleShare create(Schedule schedule, User sharedUser) {
        ScheduleShare scheduleShare = new ScheduleShare();

        scheduleShare.schedule = schedule;
        scheduleShare.sharedUser = sharedUser;
        scheduleShare.scheduleShareStatus = ScheduleShareStatus.ACTIVE;

        return scheduleShare;
    }

    public void cancelShare() {
        if (scheduleShareStatus != ScheduleShareStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.INVALID_SCHEDULE_SHARE_STATUS);
        }

        scheduleShareStatus = ScheduleShareStatus.CANCELED;

        delete();
    }

    public void reShare() {
        if (scheduleShareStatus != ScheduleShareStatus.CANCELED) {
            throw new BaseException(ErrorEnum.INVALID_SCHEDULE_SHARE_STATUS);
        }

        scheduleShareStatus = ScheduleShareStatus.ACTIVE;

        restore();
    }

    public void deletedSchedule() {
        if (scheduleShareStatus == ScheduleShareStatus.SCHEDULE_DELETED) {
            throw new BaseException(ErrorEnum.SCHEDULE_SHARE_ALREADY_DELETED);
        }

        scheduleShareStatus = ScheduleShareStatus.SCHEDULE_DELETED;

        delete();
    }
}
