package com.example.schedulebook.domain.scheduleparticipant.dto.response;

import com.example.schedulebook.domain.scheduleparticipant.enums.AttendanceStatus;
import com.example.schedulebook.domain.scheduleparticipant.projection.ScheduleParticipantProjection;

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
