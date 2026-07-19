package com.example.schedulebook.domain.auth.event;

import com.example.schedulebook.domain.auth.enums.AuditEventType;

public record ForceLogoutAuditEvent(
        Long adminId,
        Long userId,
        AuditEventType auditEventType,
        String ip,
        String userAgent
) {
}
