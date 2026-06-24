package com.example.schedulebook.domain.chat.dto.response;

import com.example.schedulebook.domain.chat.projection.MemberReadStatusProjection;

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
