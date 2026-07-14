package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.domain.auth.entity.AuditLog;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
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

    public void saveLoginSuccess(LoginSuccessEvent event) {
        AuditLog auditLog = AuditLog.create(
                event.userId(),
                event.loginId(),
                AuditEventType.LOGIN_SUCCESS,
                event.ip(),
                event.userAgent()
        );

        auditLogRepository.save(auditLog);
    }

    public void saveLoginFailed(LoginFailedEvent event) {
        AuditLog auditLog = AuditLog.create(
                null,
                event.loginId(),
                AuditEventType.LOGIN_FAILED,
                event.ip(),
                event.userAgent()
        );

        auditLogRepository.save(auditLog);
    }

    public void saveLogout(LogoutEvent event) {
        AuditLog auditLog = AuditLog.create(
                event.userId(),
                null,
                AuditEventType.LOGOUT,
                event.ip(),
                event.userAgent()
        );

        auditLogRepository.save(auditLog);
    }

    public void saveReplay(RefreshReplayDetectedEvent event) {
        AuditLog auditLog = AuditLog.create(
                event.userId(),
                event.loginId(),
                AuditEventType.REFRESH_REPLAY,
                event.ip(),
                event.userAgent()
        );

        auditLogRepository.save(auditLog);
    }

    public void saveWithdraw(UserWithdrawEvent event) {
        AuditLog auditLog = AuditLog.create(
                event.userId(),
                event.loginId(),
                AuditEventType.USER_WITHDRAW,
                event.ip(),
                event.userAgent()
        );

        auditLogRepository.save(auditLog);
    }

    public void saveLogoutSession(LogoutSessionEvent event) {
        AuditLog auditLog = AuditLog.create(
                event.userId(),
                null,
                AuditEventType.SESSION_LOGOUT,
                event.ip(),
                event.userAgent()
        );

        auditLogRepository.save(auditLog);
    }
}
