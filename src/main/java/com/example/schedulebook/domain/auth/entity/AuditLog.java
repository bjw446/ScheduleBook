package com.example.schedulebook.domain.auth.entity;

import com.example.schedulebook.common.entity.CreateEntity;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog extends CreateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id")
    private String loginId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "audit_event_type")
    private AuditEventType auditEventType;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false, name = "user_agent")
    private String userAgent;

    public static AuditLog create(Long userId, String loginId, AuditEventType auditEventType, String ip, String userAgent) {
        AuditLog auditLog = new AuditLog();

        auditLog.userId = userId;
        auditLog.loginId = loginId;
        auditLog.auditEventType = auditEventType;
        auditLog.ip = ip;
        auditLog.userAgent = userAgent;

        return auditLog;
    }
}
