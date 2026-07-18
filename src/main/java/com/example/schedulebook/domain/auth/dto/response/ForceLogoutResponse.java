package com.example.schedulebook.domain.auth.dto.response;

import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;

public record ForceLogoutResponse(
        String sessionId,
        String reason,
        String message
) {
    public static ForceLogoutResponse from(ForceLogoutSessionEvent event) {
        return new ForceLogoutResponse(
                event.sessionId(),
                AuditEventType.ADMIN_ACTION.toString(),
                "다른 환경에서 로그아웃되었습니다."
        );
    }
}
