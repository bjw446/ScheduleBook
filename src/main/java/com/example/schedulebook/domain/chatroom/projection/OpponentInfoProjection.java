package com.example.schedulebook.domain.chatroom.projection;

import java.time.LocalDateTime;

public interface OpponentInfoProjection {
    String getNickname();

    LocalDateTime getUserDeletedAt();
}
