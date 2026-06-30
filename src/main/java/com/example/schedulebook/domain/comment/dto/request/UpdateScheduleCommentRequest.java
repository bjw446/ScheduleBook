package com.example.schedulebook.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateScheduleCommentRequest(
        @NotBlank
        @Size(max = 500)
        String content

) {
}
