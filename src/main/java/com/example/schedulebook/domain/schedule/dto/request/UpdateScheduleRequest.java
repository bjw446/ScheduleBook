package com.example.schedulebook.domain.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateScheduleRequest(
        @NotBlank(message = "일정 제목은 필수 입력사항 입니다.")
        @Size(max = 50, message = "일정 제목은 최대 50자 까지 입력 가능합니다.")
        String title,

        @NotBlank(message = "일정 내용은 필수 입력사항 입니다.")
        @Size(max = 1000, message = "일정 내용은 최대 1000자 까지 입력 가능합니다.")
        String content,

        @NotNull(message = "일정 날짜는 필수 입력사항 입니다.")
        LocalDate scheduleDate,

        LocalTime startTime,

        LocalTime endTime
) {
}
