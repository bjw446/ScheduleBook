package com.example.schedulebook.domain.chat.projection;

import java.time.LocalDateTime;

public interface MemberReadStatusProjection {
    Long getUserId();

    String getNickname();

    Long getLastReadMessageId();

    LocalDateTime getJoinedAt();
}
