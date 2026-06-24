package com.example.schedulebook.domain.chat.projection;

public interface MemberReadStatusProjection {
    Long getUserId();

    String getNickname();

    Long getLastReadMessageId();
}
