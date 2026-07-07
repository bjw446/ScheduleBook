package com.example.schedulebook.domain.schedule_share.dto.request;

import com.example.schedulebook.domain.schedule_participant.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceRequest(
        @NotNull
        AttendanceStatus attendanceStatus
) {
}
