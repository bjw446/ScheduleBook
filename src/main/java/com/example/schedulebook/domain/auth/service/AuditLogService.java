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
        save(event.userId(), null, event.loginId(), AuditEventType.LOGIN_SUCCESS, event.ip(), event.userAgent());
    }

    public void saveLoginFailed(LoginFailedEvent event) {
        save(null, null, event.loginId(), AuditEventType.LOGIN_FAILED, event.ip(), event.userAgent());
    }

    public void saveLogout(LogoutEvent event) {
        save(event.userId(), null, null, AuditEventType.LOGOUT, event.ip(), event.userAgent());
    }

    public void saveReplay(RefreshReplayDetectedEvent event) {
        save(event.userId(), null, event.loginId(), AuditEventType.REFRESH_REPLAY, event.ip(), event.userAgent());
    }

    public void saveWithdraw(UserWithdrawEvent event) {
        save(event.userId(), null, event.loginId(), AuditEventType.USER_WITHDRAW, event.ip(), event.userAgent());
    }

    public void saveLogoutSession(LogoutSessionEvent event) {
        save(event.userId(), null, null, AuditEventType.SESSION_LOGOUT, event.ip(), event.userAgent());
    }

    public void saveForceLogout(ForceLogoutAuditEvent event) {
        save(event.userId(), event.adminId(), null, event.auditEventType(), event.ip(), event.userAgent());
    }

    private void save(Long userId, Long adminId, String loginId, AuditEventType auditEventType, String ip, String userAgent) {
        AuditLog auditLog = AuditLog.create(
                userId,
                adminId,
                loginId,
                auditEventType,
                auditEventType.getDescription(),
                ip,
                userAgent
        );

        auditLogRepository.save(auditLog);
    }
}
