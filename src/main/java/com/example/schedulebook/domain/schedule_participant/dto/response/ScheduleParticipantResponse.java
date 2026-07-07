package com.example.schedulebook.domain.schedule_participant.dto.response;

import com.example.schedulebook.domain.schedule_participant.enums.AttendanceStatus;
import com.example.schedulebook.domain.schedule_participant.projection.ScheduleParticipantProjection;

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
