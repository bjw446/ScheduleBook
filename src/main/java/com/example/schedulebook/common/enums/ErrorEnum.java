package com.example.schedulebook.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorEnum {

    // Common
    INVALID_INPUT(400, "잘못된 입력값입니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    NOT_FOUND(404, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다."),
    LOCK_ACQUISITION_FAILED(500, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    REDIS_UNAVAILABLE(503, "Redis 서버에 연결할 수 없습니다."),
    INVALID_ARGUMENT(400, "요청값이 올바르지 않습니다"),
    DATA_CONFLICT(409, "요청이 현재 데이터 상태와 충돌합니다."),

    // USER
    LOGIN_FAILED(401, "사용자 정보가 일치하지 않습니다."),
    LOGIN_CONFLICT(409, "이미 로그인 되어 있습니다."),
    USER_NOT_ACTIVE(400, "활성화된 사용자가 아닙니다."),
    USER_ALREADY_WITHDRAW(409, "이미 탈퇴한 회원 입니다."),
    LOGIN_ID_ALREADY_EXISTS(400, "이미 사용중인 아이디 입니다."),
    EMAIL_ALREADY_EXISTS(400, "이미 사용중인 이메일 입니다."),
    NICKNAME_ALREADY_EXISTS(400, "이미 사용중인 닉네임 입니다."),
    PHONE_NUMBER_ALREADY_EXISTS(400, "이미 사용중인 핸드폰 번호 입니다."),
    USER_NOT_FOUND(404, "존재하지 않는 사용자 입니다."),
    PASSWORD_NOT_MATCH(401, "비밀번호가 일치하지 않습니다."),
    PASSWORD_SAME_AS_OLD(400, "기존 비밀번호와 동일하게 변경할 수 없습니다."),

    // Token
    TOKEN_EXPIRED(401, "만료된 토큰입니다."),
    TOKEN_INVALID(401, "유효하지 않은 토큰입니다."),
    TOKEN_MISSING(401, "토큰이 존재하지 않습니다."),

    // Redis
    REDIS_LOCK_CONFLICT(409, "현재 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),
    REDIS_LOCK_INTERRUPTED(409, "요청 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // Rate Limit
    LOGIN_RATE_LIMITED(429, "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // Notification
    NOTIFICATION_NOT_FOUND(404, "알림이 존재하지 않습니다"),
    NOTIFICATION_ALREADY_READ(409, "이미 읽은 알림입니다."),
    NOTIFICATION_FORBIDDEN(403, "해당 알림에 대한 접근 권한이 없습니다."),

    // Schedule
    INVALID_SCHEDULE_TIME(400, "잘못된 일정 시간 입니다."),
    INVALID_SCHEDULE_MONTH(400, "유효하지 않은 월입니다. 1월부터 12월까지만 입력 가능합니다."),
    SCHEDULE_NOT_FOUND(404, "존재하지 않는 일정 입니다."),
    SCHEDULE_FORBIDDEN(403, "해당 일정에 대한 접근 권한이 없습니다."),

    // Friend
    CANNOT_ADD_MYSELF(400, "자기 자신에게 친구 신청할 수 없습니다."),
    FRIEND_ALREADY_EXISTS(400, "이미 친구인 회원 입니다."),
    FRIEND_ALREADY_REQUEST(400, "이미 친구 요청한 회원 입니다."),
    FRIEND_NOT_FOUND(404, "친구 요청을 찾을 수 없습니다."),
    FRIEND_FORBIDDEN(403, "해당 친구 요청에 대한 권한이 없습니다."),
    FRIEND_ALREADY_ACCEPTED(409, "이미 수락된 친구 요청입니다."),
    FRIEND_ALREADY_REJECTED(409, "이미 거절된 친구 요청입니다."),
    FRIEND_ALREADY_BLOCKED(409, "이미 차단된 회원 입니다."),
    INVALID_FRIEND_STATUS(400, "잘못된 친구 요청 상태 입니다."),
    FRIEND_ALREADY_DELETED(409, "이미 삭제한 친구 입니다."),

    // ScheduleShare
    CANNOT_SHARE_MYSELF(400, "자기 자신에게 일정 공유를 할 수 없습니다."),
    SCHEDULE_ALREADY_SHARED(400, "이미 공유한 일정 입니다."),
    SCHEDULE_SHARE_ALREADY_DELETED(409, "이미 삭제한 일정 공유 입니다."),
    SCHEDULE_SHARE_NOT_FOUND(404, "존재하지 않는 일정 공유 입니다."),
    INVALID_SCHEDULE_SHARE_STATUS(400, "잘못된 일정 공유 상태 입니다."),

    // Presence
    PRESENCE_ACCESS_DENIED(403, "접속 여부에 접근할 권한이 없습니다."),

    // Chat
    CHAT_ROOM_FORBIDDEN(403, "해당 채팅방에 대한 접근 권한이 없습니다."),
    CHAT_ROOM_NOT_FOUND(404, "채팅방이 존재하지 않습니다."),
    CHAT_ROOM_ALREADY_EXISTS(409, "채팅방이 이미 존재 합니다."),
    INVALID_SENDER_TYPE(400, "잘못된 SENDER_TYPE 입니다."),
    INVALID_MESSAGE_TYPE(400, "잘못된 MESSAGE_TYPE 입니다."),
    INVALID_CHAT_TARGET(400, "잘못된 채팅 상대 입니다."),
    CHAT_MESSAGE_EMPTY(400, "채팅 메시지를 입력해야 합니다."),
    CHAT_MESSAGE_TOO_LONG(400, "채팅 메시지는 1000자를 초과할 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(404, "채팅 메시지가 존재하지 않습니다."),
    INVALID_REPLY_MESSAGE(400, "잘못된 답장 메시지 입니다."),
    CHAT_MESSAGE_FORBIDDEN(403, "해당 메시지에 대한 접근 권한이 없습니다.");

    private final int status;
    private final String message;
}
