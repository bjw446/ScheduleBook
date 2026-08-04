package com.example.schedulebook.domain.notification.service;

import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.enums.NotificationEventType;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserRole;
import com.example.schedulebook.domain.user.repository.UserRepository;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityNotificationService {
    private final NotificationRepository notificationRepository;
    private final UserValidator userValidator;
    private final OutboxService outboxService;
    private final UserRepository userRepository;

    @Transactional
    public void notifyUser(Long outboxId, AuditEvent event) {
        User receiver = userValidator.validateActiveUser(event.userId());

        createAndPublishNotification(receiver, NotificationType.REFRESH_REPLAY_USER, event, outboxId);
    }

    @Transactional
    public void notifyAdmin(User admin, Long outboxId, AuditEvent event) {
        createAndPublishNotification(admin, NotificationType.REFRESH_REPLAY_ADMIN, event, outboxId);
    }

    @Transactional(readOnly = true)
    public List<User> getActiveAdmins() {
        return userRepository.findAllActiveAdmins(UserRole.SUPER_ADMIN);
    }

    private void createAndPublishNotification(User user, NotificationType notificationType, AuditEvent event, Long outboxId) {
        if (notificationRepository.existsNotification(user.getId(), outboxId, notificationType)) {
            return;
        }

        String message;

        String loginId = event.loginId() == null ? "UNKNOWN" : event.loginId();

        if (notificationType == NotificationType.REFRESH_REPLAY_ADMIN) {
            message = loginId + notificationType.getDefaultMessage();

        } else {
            message = notificationType.getDefaultMessage();
        }

        Notification notification = Notification.create(
                user,
                notificationType,
                notificationType.getTitle(),
                message,
                outboxId
        );

        notificationRepository.save(notification);

        NotificationEventResponse notificationEventResponse = new NotificationEventResponse(
                NotificationEventType.CREATED,
                user.getId(),
                notification.getId(),
                notificationType.name(),
                notification.getTitle(),
                message,
                notificationRepository.countUnreadNotifications(user.getId()),
                System.currentTimeMillis()
        );

        outboxService.save(
                OutboxAggregateType.NOTIFICATION,
                String.valueOf(notification.getId()),
                OutboxEventType.NOTIFICATION_EVENT,
                notificationEventResponse
        );
    }
}
