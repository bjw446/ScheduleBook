package com.example.schedulebook.domain.schedule_share.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ScheduleShareRequest(
        @NotNull
        @Positive
        Long friendId
) {
}
