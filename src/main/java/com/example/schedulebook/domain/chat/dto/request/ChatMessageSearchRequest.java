package com.example.schedulebook.domain.chat.dto.request;

public record ChatMessageSearchRequest(
        Long cursor,
        Integer size
) {
}
