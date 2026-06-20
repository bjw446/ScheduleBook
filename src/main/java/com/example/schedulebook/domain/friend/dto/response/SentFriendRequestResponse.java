package com.example.schedulebook.domain.friend.dto.response;

import com.example.schedulebook.domain.friend.entity.Friend;

public record SentFriendRequestResponse(
        Long friendId,
        Long requesterId,
        String nickname
) {
    public static SentFriendRequestResponse from(Friend friend) {
        return new SentFriendRequestResponse(
                friend.getId(),
                friend.getRequester().getId(),
                friend.getRequester().getNickname()
        );
    }
}
