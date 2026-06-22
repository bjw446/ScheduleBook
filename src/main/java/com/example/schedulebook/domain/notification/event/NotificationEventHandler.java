package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestEvent;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.schedule.event.ScheduleSharedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventHandler {
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequest(FriendRequestEvent event) {
        try {
            notificationService.createFriendRequestNotification(event.receiverId(), event.requesterNickname(), event.friendId());

            publishToRedis(event.receiverId(), NotificationType.FRIEND_REQUEST, event.requesterNickname());
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

            publishToRedis(event.requesterId(), NotificationType.FRIEND_ACCEPTED, event.accepterNickname());
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

            publishToRedis(event.receiverId(), NotificationType.SCHEDULE_SHARED, event.ownerNickname());
        } catch (Exception e) {
            log.error("일정 공유 알림 비동기 처리 중 오류 발생 Event : {}", event, e);

            throw e;
        }

    }

    private void publishToRedis(Long receiverId, NotificationType notificationType, String senderNickname) {
        String topic = "user:notification:" + receiverId;

        String fullMessage = senderNickname + notificationType.getDefaultMessage();

        Map<String, String> messageBody = Map.of(
                "type", notificationType.name(),
                "title", notificationType.getTitle(),
                "message", fullMessage
        );

        try {
            redisTemplate.convertAndSend(topic, messageBody);
        } catch (Exception e) {
            log.error("Redis Pub/Sub 메시지 발행 실패 Topic : {}, Message : {}", topic, messageBody, e);

            throw e;
        }


    }
}
