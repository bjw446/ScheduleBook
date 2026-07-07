package com.example.schedulebook.domain.chatroom.projection;

import java.time.LocalDateTime;

public interface MemberReadStatusProjection {
    Long getUserId();

    String getNickname();

    Long getLastReadMessageId();

    LocalDateTime getJoinedAt();
}
