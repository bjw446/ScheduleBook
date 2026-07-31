package com.example.schedulebook.domain.auth.event;

import com.example.schedulebook.domain.auth.enums.AuditEventType;

public record AuditEvent(
        Long userId,
        Long adminId,
        String loginId,
        AuditEventType eventType,
        String ip,
        String userAgent
) {
}
