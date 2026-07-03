package com.example.schedulebook.domain.schedule.projection;

import com.example.schedulebook.domain.schedule.enums.AttendanceStatus;

public interface ScheduleParticipantProjection {
    Long getUserId();
    String getNickname();
    boolean isOwner();
    AttendanceStatus getAttendanceStatus();
}
