package com.example.schedulebook.domain.friend.dto.response;

import com.example.schedulebook.domain.friend.entity.Friend;

public record SentFriendRequestResponse(
        Long friendId,
        Long receiverId,
        String nickname
) {
    public static SentFriendRequestResponse from(Friend friend) {
        return new SentFriendRequestResponse(
                friend.getId(),
                friend.getReceiver().getId(),
                friend.getReceiver().getNickname()
        );
    }
}
