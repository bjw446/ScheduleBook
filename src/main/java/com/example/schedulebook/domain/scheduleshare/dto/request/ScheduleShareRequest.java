package com.example.schedulebook.domain.scheduleshare.dto.request;

import jakarta.validation.constraints.NotNull;

public record ScheduleShareRequest(
        @NotNull
        Long friendId
) {
}
