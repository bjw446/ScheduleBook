package com.example.schedulebook.domain.auth.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuditEventType {
    LOGIN_SUCCESS("로그인 성공"),
    LOGIN_FAILED("로그인 실패"),
    LOGOUT("로그아웃"),
    SESSION_LOGOUT("세션 로그아웃"),
    REFRESH_REPLAY("토큰 재사용"),
    USER_WITHDRAW("회원 탈퇴"),
    ADMIN_ACTION("관리자에 의한 제재"),
    FORCE_LOGOUT("강제 로그아웃"),
    FORCE_LOGOUT_ALL("전체 강제 로그아웃");

    private final String description;
}
