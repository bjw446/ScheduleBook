package com.example.schedulebook.domain.chat.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChatRoomInviteRequest(
        @NotEmpty
        List<Long> memberIds
) {
}
