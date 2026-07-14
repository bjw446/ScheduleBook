package com.example.schedulebook.domain.auth.event;

import com.example.schedulebook.domain.auth.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogListener {
    private final AuditLogService auditLogService;

    @EventListener
    public void handle(LoginSuccessEvent event) {
        try {
            auditLogService.saveLoginSuccess(event);

        } catch (Exception e) {
            log.error("로그인 성공 로그 저장 에러 발생 : {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handle(LoginFailedEvent event) {
        try {
            auditLogService.saveLoginFailed(event);

        } catch (Exception e) {
            log.error("로그인 실패 로그 저장 에러 발생 : {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handle(LogoutEvent event) {
        try {
            auditLogService.saveLogout(event);

        } catch (Exception e) {
            log.error("로그아웃 로그 저장 에러 발생 : {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handle(RefreshReplayDetectedEvent event) {
        try {
            auditLogService.saveReplay(event);

        } catch (Exception e) {
            log.error("리프레시 재사용 로그 저장 에러 발생 : {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handle(UserWithdrawEvent event) {
        try {
            auditLogService.saveWithdraw(event);

        } catch (Exception e) {
            log.error("회원 탈퇴 로그 저장 에러 발생 : {}", e.getMessage(), e);
        }
    }
}
