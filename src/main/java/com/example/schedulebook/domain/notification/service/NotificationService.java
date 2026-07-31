package com.example.schedulebook.domain.notification.service;

import com.example.schedulebook.domain.notification.dto.response.NotificationDetailResponse;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.notification.dto.response.NotificationSummaryResponse;
import com.example.schedulebook.domain.notification.dto.response.UnreadNotificationCountResponse;
import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.enums.NotificationEventType;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import com.example.schedulebook.domain.notification.validator.NotificationValidator;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserValidator userValidator;
    private final NotificationValidator notificationValidator;
    private final OutboxService outboxService;

    public void createFriendRequestNotification(Long receiverId, String requesterNickname, Long friendId) {
        createNotification(
                receiverId,
                NotificationType.FRIEND_REQUEST,
                NotificationType.FRIEND_REQUEST.getTitle(),
                requesterNickname + NotificationType.FRIEND_REQUEST.getDefaultMessage(),
                friendId
        );
    }

    public void createFriendAcceptedNotification(Long requesterId, String acceptedNickname, Long friendId) {
        createNotification(
                requesterId,
                NotificationType.FRIEND_ACCEPTED,
                NotificationType.FRIEND_ACCEPTED.getTitle(),
                acceptedNickname + NotificationType.FRIEND_ACCEPTED.getDefaultMessage(),
                friendId
        );
    }

    public void createScheduleSharedNotification(Long receiverId, String ownerNickname, Long shareId) {
        createNotification(
                receiverId,
                NotificationType.SCHEDULE_SHARED,
                NotificationType.SCHEDULE_SHARED.getTitle(),
                ownerNickname + NotificationType.SCHEDULE_SHARED.getDefaultMessage(),
                shareId
        );
    }

    public void createScheduleReminderNotification(Long receiverId, Long scheduleId, String scheduleTitle) {
        createNotification(
                receiverId,
                NotificationType.SCHEDULE_REMINDER,
                NotificationType.SCHEDULE_REMINDER.getTitle(),
                scheduleTitle + NotificationType.SCHEDULE_REMINDER.getDefaultMessage(),
                scheduleId
        );
    }

    public void createScheduleCommentNotification(Long receiverId, String writerNickname, Long scheduleId) {
        if (!notificationRepository.existsNotification(
                receiverId,
                scheduleId,
                NotificationType.SCHEDULE_COMMENT
        )) {
            createNotification(
                    receiverId,
                    NotificationType.SCHEDULE_COMMENT,
                    NotificationType.SCHEDULE_COMMENT.getTitle(),
                    writerNickname + NotificationType.SCHEDULE_COMMENT.getDefaultMessage(),
                    scheduleId
            );
        }
    }

    public void createCommentReplyNotification(Long receiverId, String writerNickname, Long scheduleId) {
        createNotification(
                receiverId,
                NotificationType.COMMENT_REPLY,
                NotificationType.COMMENT_REPLY.getTitle(),
                writerNickname + NotificationType.COMMENT_REPLY.getDefaultMessage(),
                scheduleId
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationSummaryResponse> findAllMyNotification(Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        List<Notification> notifications = notificationRepository.findAllByReceiverId(currentUserId);

        return notifications.stream()
                .map(NotificationSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse findOneMyNotification(Long notificationId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        Notification notification = notificationValidator.validateOwnedNotification(notificationId, currentUserId);

        return NotificationDetailResponse.from(notification);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        long count = notificationRepository.countUnreadNotifications(currentUserId);

        return UnreadNotificationCountResponse.from(count);
    }

    public void readNotification(Long notificationId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        Notification notification = notificationValidator.validateOwnedNotification(notificationId, currentUserId);

        notification.read();

        NotificationEventResponse notificationEventResponse = new NotificationEventResponse(
                NotificationEventType.READ,
                currentUserId,
                notificationId,
                null,
                null,
                null,
                notificationRepository.countUnreadNotifications(currentUserId),
                System.currentTimeMillis()
        );

        outboxService.save(
                OutboxAggregateType.NOTIFICATION,
                currentUserId,
                OutboxEventType.NOTIFICATION_EVENT,
                notificationEventResponse
        );
    }

    public void readAllNotifications(Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        notificationRepository.readAllNotifications(currentUserId);

        NotificationEventResponse notificationEventResponse = new NotificationEventResponse(
                NotificationEventType.ALL_READ,
                currentUserId,
                null,
                null,
                null,
                null,
                notificationRepository.countUnreadNotifications(currentUserId),
                System.currentTimeMillis()
        );

        outboxService.save(
                OutboxAggregateType.NOTIFICATION,
                currentUserId,
                OutboxEventType.NOTIFICATION_EVENT,
                notificationEventResponse
        );
    }

    public void deleteAllNotifications(Long userId) {
        notificationRepository.softDeleteAllByReceiverId(userId);
    }

    private Notification createNotification(Long receiverId, NotificationType notificationType, String title, String content, Long targetId) {
        User receiver = userValidator.validateActiveUser(receiverId);

        return saveNotification(receiver, notificationType, title, content, targetId);
    }

    private Notification saveNotification(User receiver, NotificationType notificationType, String title, String content, Long targetId) {
        Notification notification = Notification.create(
                receiver,
                notificationType,
                title,
                content,
                targetId
        );

        notificationRepository.save(notification);

        NotificationEventResponse notificationEventResponse = new NotificationEventResponse(
                NotificationEventType.CREATED,
                receiver.getId(),
                notification.getId(),
                notificationType.name(),
                title,
                content,
                notificationRepository.countUnreadNotifications(receiver.getId()),
                System.currentTimeMillis()
        );

        outboxService.save(
                OutboxAggregateType.NOTIFICATION,
                notification.getId(),
                OutboxEventType.NOTIFICATION_EVENT,
                notificationEventResponse
        );

        return notification;
    }
}
