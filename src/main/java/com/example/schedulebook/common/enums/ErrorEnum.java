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
    USER_NOT_ACTIVE(400, "활성화된 사용자가 아닙니다."),
    USER_ALREADY_WITHDRAW(409, "이미 탈퇴한 회원 입니다."),

    // Token
    TOKEN_EXPIRED(401, "만료된 토큰입니다."),
    TOKEN_INVALID(401, "유효하지 않은 토큰입니다."),

    // Redis
    REDIS_LOCK_CONFLICT(409, "현재 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),
    REDIS_LOCK_INTERRUPTED(409, "요청 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // Rate Limit
    LOGIN_RATE_LIMITED(429, "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // Notification
    NOTIFICATION_NOT_FOUND(404, "알림이 존재하지 않습니다");

    private final int status;
    private final String message;
}
