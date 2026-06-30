package com.example.schedulebook.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateScheduleCommentRequest(
        @NotBlank
        @Size(max = 1000)
        String content

) {
}
