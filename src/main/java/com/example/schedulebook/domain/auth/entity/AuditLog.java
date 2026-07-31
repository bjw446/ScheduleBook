package com.example.schedulebook.domain.auth.entity;

import com.example.schedulebook.common.entity.CreateEntity;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "audit_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_audit_outbox",
                        columnNames = {"outbox_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog extends CreateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "login_id")
    private String loginId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "audit_event_type")
    private AuditEventType auditEventType;

    private String description;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false, name = "user_agent")
    private String userAgent;

    @Column(name = "outbox_id", nullable = false)
    private Long outboxId;

    public static AuditLog create(
            Long userId,
            Long adminId,
            String loginId,
            AuditEventType auditEventType,
            String description,
            String ip,
            String userAgent,
            Long outboxId
    ) {
        AuditLog auditLog = new AuditLog();

        auditLog.userId = userId;
        auditLog.adminId = adminId;
        auditLog.loginId = loginId;
        auditLog.auditEventType = auditEventType;
        auditLog.description = description;
        auditLog.ip = ip;
        auditLog.userAgent = userAgent;
        auditLog.outboxId = outboxId;

        return auditLog;
    }
}
