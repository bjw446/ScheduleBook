package com.example.schedulebook.domain.schedule.dto.response;

import com.example.schedulebook.domain.schedule.entity.ScheduleParticipant;
import com.example.schedulebook.domain.schedule.enums.AttendanceStatus;

public record ScheduleParticipantResponse(
        Long userId,
        String nickname,
        boolean owner,
        AttendanceStatus attendanceStatus
) {
    public static ScheduleParticipantResponse from(ScheduleParticipant scheduleParticipant) {
        return new ScheduleParticipantResponse(
                scheduleParticipant.getUser().getId(),
                scheduleParticipant.getUser().getNickname(),
                scheduleParticipant.getSchedule().getUser().getId().equals(scheduleParticipant.getUser().getId()),
                scheduleParticipant.getAttendanceStatus()
        );
    }
}
