package com.example.schedulebook.domain.friend.event;

public record FriendRequestEvent(
        Long receiverId,
        String requesterNickname,
        Long friendId
) {
}
