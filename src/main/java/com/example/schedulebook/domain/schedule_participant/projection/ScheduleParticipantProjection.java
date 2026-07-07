package com.example.schedulebook.domain.schedule_participant.projection;

import com.example.schedulebook.domain.schedule_participant.enums.AttendanceStatus;

public interface ScheduleParticipantProjection {
    Long getUserId();
    String getNickname();
    boolean isOwner();
    AttendanceStatus getAttendanceStatus();
}
