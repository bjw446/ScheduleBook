package com.example.schedulebook.domain.chat_message.dto.response;

import com.example.schedulebook.domain.chat_room.projection.MemberReadStatusProjection;

public record MemberReadStatusResponse(
        Long userId,
        String nickname,
        Long lastReadMessageId
) {
    public static MemberReadStatusResponse from(MemberReadStatusProjection memberReadStatusProjection) {
        return new MemberReadStatusResponse(
                memberReadStatusProjection.getUserId(),
                memberReadStatusProjection.getNickname(),
                memberReadStatusProjection.getLastReadMessageId()
        );
    }
}
