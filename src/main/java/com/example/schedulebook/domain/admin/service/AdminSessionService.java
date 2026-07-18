package com.example.schedulebook.domain.admin.service;

import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import com.example.schedulebook.domain.auth.service.SessionService;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminSessionService {
    private final SessionService sessionService;
    private final UserValidator userValidator;

    @Transactional(readOnly = true)
    public List<SessionInfoResponse> findAllUserSessions(Long adminId, Long userId) {
        validateAdminAndUser(adminId, userId);

        return sessionService.findSessions(userId);
    }

    public void logoutUserOneSession(Long adminId, Long userId, String sessionId) {
        validateAdminAndUser(adminId, userId);

        sessionService.forceLogoutSession(userId, sessionId);
    }

    public void logoutUserAllSession(Long adminId, Long userId) {
        validateAdminAndUser(adminId, userId);

        sessionService.forceLogoutAllSessions(userId);
    }

    private void validateAdminAndUser(Long adminId, Long userId) {
        userValidator.validateActiveAdmin(adminId);

        userValidator.validateActiveUser(userId);
    }
}
