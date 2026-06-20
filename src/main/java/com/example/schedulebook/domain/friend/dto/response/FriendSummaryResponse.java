package com.example.schedulebook.domain.friend.dto.response;

import com.example.schedulebook.domain.user.entity.User;

public record FriendSummaryResponse(
        Long friendId,
        Long userId,
        String nickname,
        int level
) {
    public static FriendSummaryResponse from(Long friendId, User friendUser) {
        return new FriendSummaryResponse(
                friendId,
                friendUser.getId(),
                friendUser.getNickname(),
                friendUser.getLevel()
        );
    }
}
