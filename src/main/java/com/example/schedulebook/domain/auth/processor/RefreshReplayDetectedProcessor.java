package com.example.schedulebook.domain.auth.processor;

import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.notification.service.SecurityNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshReplayDetectedProcessor {
    private final SecurityNotificationService securityNotificationService;

    public void process(AuditEvent event) {
        try {
            securityNotificationService.notifyUser(event);

        } catch (Exception e) {
            log.error("리프레시 재사용 감시 사용자 전달 실패 : {}", e.getMessage(), e);
        }

        try {
            securityNotificationService.notifyAdmins(event);

        } catch (Exception e) {
            log.error("리프레시 재사용 감시 운영자 전달 실패 : {}", e.getMessage(), e);
        }
    }
}
