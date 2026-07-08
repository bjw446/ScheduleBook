package com.example.schedulebook.domain.notification.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationValidator {
    private final NotificationRepository notificationRepository;
    public Notification validateNotification(Long notificationId) {
        return notificationRepository.findByIdWithReceiver(notificationId).orElseThrow(
                () -> new BaseException(ErrorEnum.NOTIFICATION_NOT_FOUND)
        );
    }

    public void validateNotificationOwner(Notification notification, Long currentUserId) {
        if (!notification.getReceiver().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.NOTIFICATION_FORBIDDEN);
        }
    }

    public Notification validateOwnedNotification(Long notificationId, Long currentUserId) {
        Notification notification = validateNotification(notificationId);

        validateNotificationOwner(notification, currentUserId);

        return notification;
    }
}
