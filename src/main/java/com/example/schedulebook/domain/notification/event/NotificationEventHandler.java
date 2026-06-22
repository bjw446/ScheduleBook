package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestEvent;
import com.example.schedulebook.domain.notification.dto.response.NotificationRealtimeResponse;
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
        String topic = "notification";

        String fullMessage = senderNickname + notificationType.getDefaultMessage();

        NotificationRealtimeResponse response = new NotificationRealtimeResponse(
                receiverId,
                notificationType.name(),
                notificationType.getTitle(),
                fullMessage
        );

        try {
            redisTemplate.convertAndSend(topic, response);
        } catch (Exception e) {
            log.error("Redis Pub/Sub 메시지 발행 실패 Topic : {}, Message : {}", topic, response, e);

            throw e;
        }
    }
}
