package com.example.schedulebook.domain.schedule.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateScheduleRequest(
        @Size(max = 50, message = "일정 제목은 최대 50자 까지 입력 가능합니다.")
        String title,

        @Size(max = 1000, message = "일정 제목은 최대 1000자 까지 입력 가능합니다.")
        String content,

        LocalDate scheduleDate,

        LocalTime startTime,

        LocalTime endTime
) {
}
