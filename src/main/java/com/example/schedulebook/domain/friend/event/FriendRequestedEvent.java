package com.example.schedulebook.domain.friend.event;

public record FriendRequestedEvent(
        Long receiverId,
        String requesterNickname,
        Long friendId
) {
}
