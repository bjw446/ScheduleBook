package com.example.schedulebook.domain.friend.dto.request;

import jakarta.validation.constraints.NotNull;

public record FriendRequest(
        @NotNull
        Long receiverId
) {
}
