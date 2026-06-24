package com.example.schedulebook.domain.chat.projection;

import java.time.LocalDateTime;

public interface OpponentInfoProjection {
    String getNickname();

    LocalDateTime getUserDeletedAt();
}
