package com.example.schedulebook.domain.friend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FriendStatus {
    PENDING("친구 요청"),
    ACCEPTED("친구 수락"),
    REJECTED("친구 거절"),
    BLOCKED("친구 차단"),
    DELETED("친구 삭제");

    private final String description;
}
