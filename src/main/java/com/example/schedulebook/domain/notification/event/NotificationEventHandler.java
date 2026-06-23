package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestEvent;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.notification.enums.NotificationEventType;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.repository.NotificationRepository;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.schedule.event.ScheduleSharedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventHandler {
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequest(FriendRequestEvent event) {
        try {
            notificationService.createFriendRequestNotification(event.receiverId(), event.requesterNickname(), event.friendId());

            publishCreatedEvent(event.receiverId(), NotificationType.FRIEND_REQUEST, event.requesterNickname());
        } catch (Exception e) {
            log.error("친구 신청 알림 비동기 처리 중 오류 발생 Event : {}", event, e);

            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendAccepted(FriendAcceptedEvent event) {
        try {
            notificationService.createFriendAcceptedNotification(event.requesterId(), event.accepterNickname(), event.friendId());

            publishCreatedEvent(event.requesterId(), NotificationType.FRIEND_ACCEPTED, event.accepterNickname());
        } catch (Exception e) {
            log.error("친구 수락 알림 비동기 처리 중 오류 발생 Event : {}", event, e);

            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleScheduleShared(ScheduleSharedEvent event) {
        try {
            notificationService.createScheduleSharedNotification(event.receiverId(), event.ownerNickname(), event.shareId());

            publishCreatedEvent(event.receiverId(), NotificationType.SCHEDULE_SHARED, event.ownerNickname());
        } catch (Exception e) {
            log.error("일정 공유 알림 비동기 처리 중 오류 발생 Event : {}", event, e);

            throw e;
        }
    }

    private void publishCreatedEvent(Long receiverId, NotificationType notificationType, String senderNickname) {
        long unreadCount = notificationRepository.countUnreadNotifications(receiverId);

        String fullMessage = senderNickname + notificationType.getDefaultMessage();

        NotificationEventResponse response = new NotificationEventResponse(
                NotificationEventType.CREATED,
                receiverId,
                null,
                notificationType.name(),
                notificationType.getTitle(),
                fullMessage,
                unreadCount
        );

        notificationEventPublisher.publish(response);
    }
}
