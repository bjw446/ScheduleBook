package com.example.schedulebook.domain.auth.event;

import com.example.schedulebook.domain.auth.enums.LoginResult;
import com.example.schedulebook.domain.auth.service.LoginAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshReplayDetectedListener {
    private final LoginAuditService loginAuditService;

    @EventListener
    public void handle(RefreshReplayDetectedEvent event) {
        // TODO : Notification, monitoring 등 이벤트 추가 예정
        try {
            loginAuditService.save(event.loginId(), LoginResult.REFRESH_REPLAY, event.ip(), event.userAgent());

        } catch (Exception e) {
            log.error("리프레시 재사용 감시 저장 실패 : {}", e.getMessage(), e);
        }

    }
}
