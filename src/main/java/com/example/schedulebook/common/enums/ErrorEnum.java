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
    REQUEST_BODY_TOO_LARGE(413, "요청 본문이 너무 큽니다."),
    JSON_SERIALIZATION_FAILED(500, "JSON 직렬화에 실패했습니다."),
    JSON_DESERIALIZATION_FAILED(500, "JSON 역직렬화에 실패했습니다."),

    // USER
    LOGIN_FAILED(401, "사용자 정보가 일치하지 않습니다."),
    LOGIN_CONFLICT(409, "이미 로그인 되어 있습니다."),
    FORCE_LOGOUT(401, "다른 환경에서 로그아웃되어 다시 로그인해야 합니다."),
    USER_NOT_ACTIVE(400, "활성화된 사용자가 아닙니다."),
    USER_ALREADY_WITHDRAW(409, "이미 탈퇴한 회원 입니다."),
    LOGIN_ID_ALREADY_EXISTS(400, "이미 사용중인 아이디 입니다."),
    EMAIL_ALREADY_EXISTS(400, "이미 사용중인 이메일 입니다."),
    NICKNAME_ALREADY_EXISTS(400, "이미 사용중인 닉네임 입니다."),
    PHONE_NUMBER_ALREADY_EXISTS(400, "이미 사용중인 핸드폰 번호 입니다."),
    USER_NOT_FOUND(404, "존재하지 않는 사용자 입니다."),
    PASSWORD_NOT_MATCH(401, "비밀번호가 일치하지 않습니다."),
    PASSWORD_SAME_AS_OLD(400, "기존 비밀번호와 동일하게 변경할 수 없습니다."),
    ACCOUNT_LOCKED(423, "계정이 잠겨 있습니다, 잠시 후 다시 시도해주세요."),
    SESSION_NOT_FOUND(404, "존재하지 않는 세션 입니다."),
    SESSION_LIMIT_EXCEEDED(429, "로그인 가능한 환경 수를 초과했습니다. 다른 기기에서 로그아웃 후 다시 시도해주세요."),
    USER_WITHDRAW_PROCESS_FAILED(500, "회원 탈퇴 후처리에 실패했습니다."),
    FORCE_LOGOUT_RETRY_NOT_FOUND(404, "강제 로그아웃 재시도가 존재하지 않습니다"),

    // Token
    TOKEN_EXPIRED(401, "만료된 토큰입니다."),
    TOKEN_INVALID(401, "유효하지 않은 토큰입니다."),
    TOKEN_MISSING(401, "토큰이 존재하지 않습니다."),
    LOGOUT_TOKEN(401, "이미 로그아웃 된 토큰 입니다."),
    REFRESH_TOKEN_INVALID(401, "유효하지 않은 REFRESH 토큰입니다."),
    REFRESH_TOKEN_REPLAY(401, "삭제된 REFRESH 토큰 재사용이 감지되었습니다."),

    // Redis
    REDIS_LOCK_CONFLICT(409, "현재 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),
    REDIS_LOCK_INTERRUPTED(409, "요청 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // Rate Limit
    LOGIN_ID_RATE_LIMITED(429, "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),
    LOGIN_IP_RATE_LIMITED(429, "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),
    RATE_LIMITED_EXCEEDED(429, "너무 많은 요청입니다. 잠시 후 다시 시도해 주세요."),

    // Notification
    NOTIFICATION_NOT_FOUND(404, "알림이 존재하지 않습니다"),
    NOTIFICATION_RETRY_NOT_FOUND(404, "알림 재시도가 존재하지 않습니다"),
    NOTIFICATION_EVENT_NOT_FOUND(404, "알림 이벤트가 존재하지 않습니다"),
    PROCESSED_NOTIFICATION_RETRY_NOT_FOUND(404, "프로세스 알림 재시도가 존재하지 않습니다"),
    NOTIFICATION_ALREADY_READ(409, "이미 읽은 알림입니다."),
    NOTIFICATION_FORBIDDEN(403, "해당 알림에 대한 접근 권한이 없습니다."),
    INVALID_NOTIFICATION_TYPE(400, "잘못된 알림 타입 입니다."),
    NOTIFICATION_RETRY_SAVE_FAILED(500, "알림 재시도 저장을 실패했습니다."),
    NOTIFICATION_RETRY_FORBIDDEN(403, "해당 알림 재시도에 대한 권한이 없습니다."),
    PROCESSED_NOTIFICATION_RETRY_STATUS_CHANGE_FAILED(500, "프로세스 알림 재시도 상태 변경에 실패했습니다."),

    // Schedule
    INVALID_SCHEDULE_TIME(400, "잘못된 일정 시간 입니다."),
    INVALID_SCHEDULE_MONTH(400, "유효하지 않은 월입니다. 1월부터 12월까지만 입력 가능합니다."),
    CANNOT_ACCEPT_MYSELF(400, "자기 자신에 일정을 공유 받을 수 없습니다."),
    SCHEDULE_NOT_FOUND(404, "존재하지 않는 일정 입니다."),
    SCHEDULE_FORBIDDEN(403, "해당 일정에 대한 접근 권한이 없습니다."),
    SCHEDULE_REMINDER_NOT_FOUND(404, "존재하지 않는 일정 알림 입니다."),
    SCHEDULE_REMINDER_STATUS_CHANGE_FAILED(500, "일정 알림 상태 변경에 실패했습니다."),

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
    SCHEDULE_SHARE_CANCELED(404, "취소된 일정 공유 입니다."),
    INVALID_SCHEDULE_SHARE_STATUS(400, "잘못된 일정 공유 상태 입니다."),

    // Presence
    PRESENCE_ACCESS_DENIED(403, "접속 여부에 접근할 권한이 없습니다."),

    // Chat
    CHAT_ROOM_FORBIDDEN(403, "해당 채팅방에 대한 접근 권한이 없습니다."),
    CHAT_ROOM_NOT_FOUND(404, "채팅방이 존재하지 않습니다."),
    CHAT_ROOM_ALREADY_EXISTS(409, "채팅방이 이미 존재 합니다."),
    CHAT_ROOM_MEMBER_COUNT_UPDATE_FAILED(409, "채팅방 멤버 수 변경에 실패했습니다."),
    CHAT_ROOM_MEMBER_ALREADY_EXISTS(409, "채팅멤버가 이미 존재 합니다."),
    INVALID_SENDER_TYPE(400, "잘못된 SENDER_TYPE 입니다."),
    INVALID_MESSAGE_TYPE(400, "잘못된 MESSAGE_TYPE 입니다."),
    INVALID_CHAT_TARGET(400, "잘못된 채팅 상대 입니다."),
    INVALID_CHAT_ROOM_TYPE(400, "잘못된 채팅방 타입 입니다."),
    INVALID_CURSOR(400, "잘못된 커서입니다."),
    CHAT_MESSAGE_EMPTY(400, "채팅 메시지를 입력해야 합니다."),
    CHAT_MESSAGE_TOO_LONG(400, "채팅 메시지는 1000자를 초과할 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(404, "채팅 메시지가 존재하지 않습니다."),
    CHAT_MESSAGE_DELETE_NOT_ALLOWED(400, "채팅 메시지를 삭제할 수 없습니다"),
    CHAT_MESSAGE_ALREADY_DELETE(409, "이미 삭제된 채팅 메시지 입니다."),
    INVALID_REPLY_MESSAGE(400, "잘못된 답장 메시지 입니다."),
    CHAT_MESSAGE_FORBIDDEN(403, "해당 메시지에 대한 접근 권한이 없습니다."),

    // ScheduleSnapshot
    SCHEDULE_SNAPSHOT_NOT_FOUND(404, "일정 스냅샷이 존재하지 않습니다."),

    // ScheduleParticipant
    INVALID_SCHEDULE_ATTENDANCE_STATUS(400, "잘못된 일정 참석 여부 상태 입니다."),
    SCHEDULE_PARTICIPANT_NOT_FOUND(404, "존재하지 않는 일정 참여자 입니다."),
    SCHEDULE_ALREADY_PARTICIPATED(409, "이미 참여한 일정 입니다."),

    // Comment
    COMMENT_NOT_FOUND(404, "댓글이 존재하지 않습니다."),
    COMMENT_ALREADY_DELETE(409, "이미 삭제된 댓글 입니다."),
    COMMENT_FORBIDDEN(403, "해당 댓글에 대한 접근 권한이 없습니다."),
    INVALID_COMMENT(400, "잘못된 댓글 입니다."),

    // Admin
    ADMIN_NOT_ACTIVE(400, "활성화된 관리자가 아닙니다."),
    ADMIN_NOT_FOUND(404, "존재하지 않는 관리자 입니다."),

    // Outbox
    INVALID_OUTBOX_STATUS(400, "잘못된 아웃박스 상태 입니다."),
    INVALID_OUTBOX_EVENT_TYPE(400, "잘못된 아웃박스 이벤트 타입 입니다."),
    INVALID_PROCESSED_OUTBOX_STAUS(400, "잘못된 프로세스 아웃박스 상태 입니다."),
    OUTBOX_PAYLOAD_SERIALIZATION_FAILED(500, "아웃박스 페이로드 직렬화에 실패했습니다."),
    PROCESSED_OUTBOX_STATUS_CHANGE_FAILED(500, "프로세스 아웃박스 상태 변경에 실패했습니다."),
    OUTBOX_NOT_FOUND(404, "존재하지 않는 아웃박스 입니다."),

    // DeadLetter
    DEAD_LETTER_NOT_FOUND(404, "존재하지 않는 DeadLetterQueue 입니다."),
    INVALID_DEAD_LETTER_AGGREGATE_TYPE(400, "잘못된 DeadLetter 집계 타입 입니다."),
    INVALID_DEAD_LETTER_TYPE(400, "잘못된 DeadLetter 타입 입니다."),
    DEAD_LETTER_RECOVER_FAILED(409, "DeadLetter 복구에 실패 했습니다."),
    DEAD_LETTER_SAVE_FAILED(409, "DeadLetter 저장에 실패 했습니다."),
    DEAD_LETTER_ALREADY_RECOVERED(409, "이미 복구된 DeadLetterQueue 입니다.");

    private final int status;
    private final String message;
}