package com.example.schedulebook.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    FRIEND_REQUEST("친구 요청", "님이 친구 요청을 보냈습니다."),
    FRIEND_ACCEPTED("친구 수락", "님이 친구 요청을 수락했습니다."),
    SCHEDULE_SHARED("일정 공유", "님이 일정을 공유했습니다."),
    SCHEDULE_REMINDER("일정 알림", " 일정이 시작되었습니다.");

    private final String title;
    private final String defaultMessage;
}