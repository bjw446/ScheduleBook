package com.example.schedulebook.domain.scheduleshare.dto.request;

import com.example.schedulebook.domain.schedule.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceRequest(
        @NotNull
        AttendanceStatus attendanceStatus
) {
}
