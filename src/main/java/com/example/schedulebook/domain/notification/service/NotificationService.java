package com.example.schedulebook.domain.notification.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.dto.response.NotificationDetailResponse;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.notification.dto.response.NotificationSummaryResponse;
import com.example.schedulebook.domain.notification.dto.response.UnreadNotificationCountResponse;
import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.enums.NotificationEventType;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.event.NotificationEventPublisher;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    public void createFriendRequestNotification(Long receiverId, String requesterNickname, Long friendId) {
        createNotification(
                receiverId,
                NotificationType.FRIEND_REQUEST,
                NotificationType.FRIEND_REQUEST.getTitle(),
                requesterNickname + NotificationType.FRIEND_REQUEST.getDefaultMessage(),
                friendId
        );
    }

    public void createFriendAcceptedNotification(Long requesterId, String accepterNickname, Long friendId) {
        createNotification(
                requesterId,
                NotificationType.FRIEND_ACCEPTED,
                NotificationType.FRIEND_ACCEPTED.getTitle(),
                accepterNickname + NotificationType.FRIEND_ACCEPTED.getDefaultMessage(),
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

    public void createScheduleReminderNotification(User receiver, Long scheduleId, String scheduleTitle) {
        createNotification(
                receiver,
                NotificationType.SCHEDULE_REMINDER,
                NotificationType.SCHEDULE_REMINDER.getTitle(),
                scheduleTitle + NotificationType.SCHEDULE_REMINDER.getDefaultMessage(),
                scheduleId
        );
    }

    public void createScheduleCommentNotification(Long receiverId, String writerNickname, Long scheduleId) {
        createNotification(
                receiverId,
                NotificationType.SCHEDULE_COMMENT,
                NotificationType.SCHEDULE_COMMENT.getTitle(),
                writerNickname + NotificationType.SCHEDULE_COMMENT.getDefaultMessage(),
                scheduleId
        );
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
        validateUser(currentUserId);

        List<Notification> notifications = notificationRepository.findAllByReceiverId(currentUserId);

        return notifications.stream()
                .map(NotificationSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse findOneMyNotification(Long notificationId, Long currentUserId) {
        validateUser(currentUserId);

        Notification notification = validateNotification(notificationId);

        validateNotificationOwner(notification, currentUserId);

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

        publishAfterCommit(() ->
                new NotificationEventResponse(
                        NotificationEventType.READ,
                        currentUserId,
                        notificationId,
                        null,
                        null,
                        null,
                        notificationRepository.countUnreadNotifications(currentUserId),
                        System.currentTimeMillis()
                )
        );
    }

    public void readAllNotifications(Long currentUserId) {
        validateUser(currentUserId);

        notificationRepository.readAllNotifications(currentUserId);

        publishAfterCommit(() ->
                new NotificationEventResponse(
                        NotificationEventType.ALL_READ,
                        currentUserId,
                        null,
                        null,
                        null,
                        null,
                        notificationRepository.countUnreadNotifications(currentUserId),
                        System.currentTimeMillis()
                )
        );
    }

    private Notification createNotification(Long receiverId, NotificationType notificationType, String title, String content, Long targetId) {
        User receiver = validateUser(receiverId);
        return saveNotification(receiver, notificationType, title, content, targetId);
    }

    private Notification createNotification(User receiver, NotificationType notificationType, String title, String content, Long targetId) {
        validateUserStatus(receiver);
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

        return notificationRepository.save(notification);
    }

    private User validateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );

        validateUserStatus(user);

        return user;
    }

    private void validateUserStatus(User user) {
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.USER_NOT_ACTIVE);
        }
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

    private void publishAfterCommit(Supplier<NotificationEventResponse> responseSupplier) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            notificationEventPublisher.publish(responseSupplier.get());

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    notificationEventPublisher.publish(responseSupplier.get());
                } catch (Exception e) {
                    log.error("커밋 후 알림 이벤트 발행 실패", e);
                }
            }
        });
    }
}
