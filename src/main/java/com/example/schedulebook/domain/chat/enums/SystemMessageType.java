package com.example.schedulebook.domain.chat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemMessageType {
    SCHEDULE_UPDATED("공유 일정이 수정되었습니다."),
    SCHEDULE_DELETED("공유 일정이 삭제되었습니다."),
    SCHEDULE_SHARE_CANCELED("공유 일정의 공유가 취소되었습니다."),
    USER_JOIN("%s님이 채팅방에 입장했습니다."),
    USER_LEAVE("%s님이 채팅방을 떠났습니다."),
    USER_INVITED("%s님이 %s을 초대했습니다."),
    GROUP_ROOM_CREATED("%s님이 그룹 채팅방을 만들었습니다."),
    ROOM_NAME_UPDATED("%s님이 채팅방 이름을 변경했습니다. (%s -> %s)");

    private final String message;

    public String format(Object... args) {
        return String.format(message, args);
    }
}
