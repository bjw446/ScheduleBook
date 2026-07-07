package com.example.schedulebook.domain.scheduleparticipant.projection;

import com.example.schedulebook.domain.scheduleparticipant.enums.AttendanceStatus;

public interface ScheduleParticipantProjection {
    Long getUserId();
    String getNickname();
    boolean isOwner();
    AttendanceStatus getAttendanceStatus();
}
