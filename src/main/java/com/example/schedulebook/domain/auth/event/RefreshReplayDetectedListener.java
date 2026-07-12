package com.example.schedulebook.domain.auth.event;

import com.example.schedulebook.domain.auth.enums.LoginResult;
import com.example.schedulebook.domain.auth.service.LoginAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class RefreshReplayDetectedListener {
    private final LoginAuditService loginAuditService;

    @EventListener
    public void handle(RefreshReplayDetectedEvent event) {
        // TODO : Notification, monitoring 등 이벤트 추가 예정
        loginAuditService.save(event.loginId(), LoginResult.REFRESH_REPLAY, event.ip(), event.userAgent());
    }
}
