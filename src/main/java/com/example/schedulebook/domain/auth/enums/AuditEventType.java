package com.example.schedulebook.domain.auth.enums;

public enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    SESSION_LOGOUT,
    REFRESH_REPLAY,
    USER_WITHDRAW,
    ADMIN_ACTION
}
