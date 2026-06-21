package com.example.schedulebook.domain.notification.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.dto.response.NotificationDetailResponse;
import com.example.schedulebook.domain.notification.dto.response.NotificationSummaryResponse;
import com.example.schedulebook.domain.notification.dto.response.UnreadNotificationCountResponse;
import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public Notification createNotification(Long receiverId, NotificationType notificationType, String title, String content, Long targetId) {
        User receiver = validateUser(receiverId);

        Notification notification = Notification.create(
                receiver,
                notificationType,
                title,
                content,
                targetId
        );

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationSummaryResponse> findAllMyNotification(Long currentUserId) {
        validateUser(currentUserId);

        List<Notification> notifications = notificationRepository.findAllByReceiverId(currentUserId);

        return notifications.stream()
                .map(NotificationSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse findOneMyNotification(Long notificationId, Long currentUserid) {
        validateUser(currentUserid);

        Notification notification = validateNotification(notificationId);

        validateNotificationOwner(notification, currentUserid);

        return NotificationDetailResponse.from(notification);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(Long currentUserId) {
        validateUser(currentUserId);

        long count = notificationRepository.countUnreadNotifications(currentUserId);

        return UnreadNotificationCountResponse.from(count);
    }

    public void readNotification(Long notificationId, Long currentUserId) {
        validateUser(currentUserId);

        Notification notification = validateNotification(notificationId);

        validateNotificationOwner(notification, currentUserId);

        notification.read();
    }

    public void readAllNotifications(Long currentUserId) {
        validateUser(currentUserId);

        notificationRepository.readAllNotification(currentUserId);
    }

    private User validateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.USER_NOT_ACTIVE);
        }

        return user;
    }

    private Notification validateNotification(Long notificationId) {
        return notificationRepository.findByIdWithReceiver(notificationId).orElseThrow(
                () -> new BaseException(ErrorEnum.NOTIFICATION_NOT_FOUND)
        );
    }

    private void validateNotificationOwner(Notification notification, Long currentUserId) {
        if (!notification.getReceiver().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.NOTIFICATION_FORBIDDEN);
        }
    }
}
