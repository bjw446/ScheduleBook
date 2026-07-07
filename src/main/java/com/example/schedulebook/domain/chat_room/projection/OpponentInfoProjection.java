package com.example.schedulebook.domain.chat_room.projection;

import java.time.LocalDateTime;

public interface OpponentInfoProjection {
    String getNickname();

    LocalDateTime getUserDeletedAt();
}
