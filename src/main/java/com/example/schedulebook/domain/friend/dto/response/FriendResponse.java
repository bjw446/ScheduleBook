package com.example.schedulebook.domain.friend.dto.response;

import com.example.schedulebook.domain.friend.entity.Friend;
import com.example.schedulebook.domain.friend.enums.FriendStatus;

public record FriendResponse(
        Long friendId,
        Long requesterId,
        Long receiverId,
        FriendStatus friendStatus
) {
    public static FriendResponse from(Friend friend) {
        return new FriendResponse(
                friend.getId(),
                friend.getRequester().getId(),
                friend.getReceiver().getId(),
                friend.getFriendStatus()
        );
    }
}
