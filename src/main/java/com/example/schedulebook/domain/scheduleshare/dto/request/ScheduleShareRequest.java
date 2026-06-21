package com.example.schedulebook.domain.scheduleshare.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ScheduleShareRequest(
        @NotNull
        @Positive
        Long friendId
) {
}
