package com.example.schedulebook.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRoomUpdateNameRequest(
        @NotBlank
        @Size(max = 100)
        String name
) {
}
