package com.example.schedulebook.domain.schedule.dto.response;

import com.example.schedulebook.domain.schedule.enums.AttendanceStatus;
import com.example.schedulebook.domain.schedule.projection.ScheduleParticipantProjection;

public record ScheduleParticipantResponse(
        Long userId,
        String nickname,
        boolean owner,
        AttendanceStatus attendanceStatus
) {
    public static ScheduleParticipantResponse from(ScheduleParticipantProjection scheduleParticipantProjection) {
        return new ScheduleParticipantResponse(
                scheduleParticipantProjection.getUserId(),
                scheduleParticipantProjection.getNickname(),
                scheduleParticipantProjection.isOwner(),
                scheduleParticipantProjection.getAttendanceStatus()
        );
    }
}
