package com.example.schedulebook.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    FRIEND_REQUEST("친구 요청"),
    FRIEND_ACCEPTED("친구 수락"),
    SCHEDULE_SHARED("일정 공유"),
    SCHEDULE_REMINDER("일정 알림");

    private final String description;
}
