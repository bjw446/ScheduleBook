package com.example.schedulebook.domain.chat.repository;

import java.time.LocalDateTime;

public interface OpponentInfoProjection {
    String getNickname();

    LocalDateTime getUserDeletedAt();
}
