package com.example.schedulebook.domain.admin.service;

import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.auth.service.SessionService;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.validator.UserValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminSessionService {
    private final SessionService sessionService;
    private final UserValidator userValidator;
    private final OutboxService outboxService;

    @Transactional(readOnly = true)
    public List<SessionInfoResponse> findAllUserSessions(Long adminId, Long userId) {
        validateAdminAndUser(adminId, userId);

        return sessionService.findSessions(userId);
    }

    public void logoutUserOneSession(Long adminId, Long userId, String sessionId, HttpServletRequest servletRequest) {
        validateAdminAndUser(adminId, userId);

        String ip = getUserIp(servletRequest);

        String userAgent = getUserAgent(servletRequest);

        String eventId = UUID.randomUUID().toString();

        sessionService.forceLogoutSession(userId, sessionId);

        outboxService.save(
                eventId,
                OutboxAggregateType.USER,
                String.valueOf(userId),
                OutboxEventType.AUDIT_EVENT,
                new AuditEvent(
                        eventId,
                        userId,
                        adminId,
                        null,
                        AuditEventType.FORCE_LOGOUT,
                        ip,
                        userAgent
                )
        );
    }

    public void logoutUserAllSession(Long adminId, Long userId, HttpServletRequest servletRequest) {
        validateAdminAndUser(adminId, userId);

        sessionService.forceLogoutAllSessions(userId);

        String ip = getUserIp(servletRequest);

        String userAgent = getUserAgent(servletRequest);

        String eventId = UUID.randomUUID().toString();

        outboxService.save(
                eventId,
                OutboxAggregateType.USER,
                String.valueOf(userId),
                OutboxEventType.AUDIT_EVENT,
                new AuditEvent(
                        eventId,
                        userId,
                        adminId,
                        null,
                        AuditEventType.FORCE_LOGOUT_ALL,
                        ip,
                        userAgent
                )
        );
    }

    private void validateAdminAndUser(Long adminId, Long userId) {
        userValidator.validateActiveAdmin(adminId);

        userValidator.validateActiveUser(userId);
    }

    private String getUserAgent(HttpServletRequest servletRequest) {
        String userAgent = servletRequest.getHeader("User-Agent");

        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "UNKNOWN";
        }

        return userAgent;
    }

    private String getUserIp(HttpServletRequest servletRequest) {
        String ip = servletRequest.getRemoteAddr();

        if (ip == null || ip.isBlank()) {
            ip = "UNKNOWN";
        }

        return ip;
    }
}
