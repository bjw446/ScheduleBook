package com.example.schedulebook.domain.chat_room.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ChatRoomInviteRequest(
        @NotEmpty
        List<@NotNull Long> memberIds
) {
}
