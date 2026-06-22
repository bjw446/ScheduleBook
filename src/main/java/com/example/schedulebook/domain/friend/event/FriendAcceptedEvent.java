package com.example.schedulebook.domain.friend.event;

public record FriendAcceptedEvent(
        Long requesterId,
        String accepterNickname,
        Long friendId
) {
}
