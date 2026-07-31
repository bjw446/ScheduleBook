package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.domain.auth.entity.AuditLog;
import com.example.schedulebook.domain.auth.event.*;
import com.example.schedulebook.domain.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public void save(AuditEvent event) {
        AuditLog auditLog = AuditLog.create(
                event.userId(),
                event.adminId(),
                event.loginId(),
                event.eventType(),
                event.eventType().getDescription(),
                event.ip(),
                event.userAgent()
        );

        auditLogRepository.save(auditLog);
    }
}
