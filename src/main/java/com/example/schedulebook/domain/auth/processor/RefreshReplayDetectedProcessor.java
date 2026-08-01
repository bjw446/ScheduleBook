package com.example.schedulebook.domain.auth.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.SecurityNotificationService;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshReplayDetectedProcessor {
    private final SecurityNotificationService securityNotificationService;
    private final NotificationRetryService notificationRetryService;

    public void process(Long outboxId, AuditEvent event) {
        try {
            securityNotificationService.notifyUser(outboxId, event);

        } catch (Exception e) {
            log.error("리프레시 재사용 감시 사용자 전달 실패 : {}", e.getMessage(), e);

            saveNotificationRetry(outboxId, event.userId(), NotificationType.REFRESH_REPLAY_USER, event, e);
        }

        List<User> admins = securityNotificationService.getActiveAdmins();

        for (User admin : admins) {
            try {
                securityNotificationService.notifyAdmin(admin, outboxId, event);

            } catch (Exception e) {
                log.error("리프레시 재사용 감시 운영자 전달 실패 : {}", e.getMessage(), e);

                saveNotificationRetry(outboxId, admin.getId(), NotificationType.REFRESH_REPLAY_ADMIN, event, e);
            }
        }
    }

    private void saveNotificationRetry(
            Long outboxId,
            Long receiverId,
            NotificationType notificationType,
            AuditEvent event,
            Exception e
    ) {
        try {
            log.error("Notification Retry 저장 outboxId = {}, receiverId = {}, type = {}",
                    outboxId,
                    receiverId,
                    notificationType,
                    e
            );

            notificationRetryService.save(
                    outboxId,
                    receiverId,
                    notificationType,
                    event,
                    e.getMessage()
            );

        } catch (Exception ex) {
            log.error("보안 알림 Retry 저장 실패", ex);

            throw new BaseException(ErrorEnum.NOTIFICATION_RETRY_SAVE_FAILED);
        }
    }
}
