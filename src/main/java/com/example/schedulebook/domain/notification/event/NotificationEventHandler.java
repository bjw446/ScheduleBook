package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestEvent;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.schedule.event.ScheduleSharedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationEventHandler {
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequest(FriendRequestEvent event) {
        notificationService.createFriendRequestNotification(event.receiverId(), event.requesterNickname(), event.friendId());

        publishToRedis(event.receiverId(), NotificationType.FRIEND_REQUEST, event.requesterNickname());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendAccepted(FriendAcceptedEvent event) {
        notificationService.createFriendAcceptedNotification(event.requesterId(), event.accepterNickname(), event.friendId());
        publishToRedis(event.requesterId(), NotificationType.FRIEND_ACCEPTED, event.accepterNickname());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleScheduleShared(ScheduleSharedEvent event) {
        notificationService.createScheduleSharedNotification(event.receiverId(), event.ownerNickname(), event.shareId());

        publishToRedis(event.receiverId(), NotificationType.SCHEDULE_SHARED, event.ownerNickname());
    }

    private void publishToRedis(Long receiverId, NotificationType notificationType, String senderNickname) {
        String topic = "user:notification:" + receiverId;

        String fullMessage = senderNickname + notificationType.getDefaultMessage();

        Map<String, String> messageBody = Map.of(
                "type", notificationType.name(),
                "title", notificationType.getTitle(),
                "message", fullMessage
        );

        redisTemplate.convertAndSend(topic, messageBody);
    }
}
