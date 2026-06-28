package com.example.schedulebook.domain.chat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemMessageType {
    SCHEDULE_UPDATED("공유 일정이 수정되었습니다."),
    SCHEDULE_DELETED("공유 일정이 삭제되었습니다."),
    SCHEDULE_SHARE_CANCELED("공유 일정의 공유가 취소되었습니다.");

    private final String message;
}
