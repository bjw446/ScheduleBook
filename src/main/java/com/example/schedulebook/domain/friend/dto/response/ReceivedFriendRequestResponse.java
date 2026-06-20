package com.example.schedulebook.domain.friend.dto.response;

import com.example.schedulebook.domain.friend.entity.Friend;

public record ReceivedFriendRequestResponse(
        Long friendId,
        Long requesterId,
        String nickname
) {
    public static ReceivedFriendRequestResponse from(Friend friend) {
        return new ReceivedFriendRequestResponse(
                friend.getId(),
                friend.getRequester().getId(),
                friend.getRequester().getNickname()
        );
    }
}
