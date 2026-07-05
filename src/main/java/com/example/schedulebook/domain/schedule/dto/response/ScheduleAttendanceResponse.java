package com.example.schedulebook.domain.schedule.dto.response;

import com.example.schedulebook.domain.schedule.enums.AttendanceStatus;

public record ScheduleAttendanceResponse(
        Long scheduleId,
        Long userId,
        String nickname,
        AttendanceStatus attendanceStatus
) {
    public static ScheduleAttendanceResponse of(Long scheduleId, Long userId, String nickname, AttendanceStatus attendanceStatus) {
        return new ScheduleAttendanceResponse(
                scheduleId,
                userId,
                nickname,
                attendanceStatus
        );
    }
}
