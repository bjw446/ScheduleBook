package com.example.schedulebook.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    FRIEND_REQUEST("친구 요청", "님이 친구 요청을 보냈습니다."),
    FRIEND_ACCEPTED("친구 수락", "님이 친구 요청을 수락했습니다."),
    SCHEDULE_SHARED("일정 공유", "님이 일정을 공유했습니다."),
    SCHEDULE_REMINDER("일정 알림", " 일정이 시작되었습니다."),
    SCHEDULE_COMMENT("일정 댓글", "님이 댓글을 남겼습니다."),
    COMMENT_REPLY("답글", "님이 회원님의 댓글에 답글을 남겼습니다."),
    REFRESH_REPLAY_USER("보안 경고", "보안상 비정상적인 인증 요청이 감지되었습니다. 필요한 경우 다시 로그인해 주세요."),
    REFRESH_REPLAY_ADMIN("토큰 재사용 감지", " 사용자 계정에서 Refresh Token 재사용이 감지되었습니다.");

    private final String title;
    private final String defaultMessage;
}