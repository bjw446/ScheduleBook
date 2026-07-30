package com.example.schedulebook.domain.friend.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.processor.NotificationEventProcessor;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FriendAcceptedProcessor implements NotificationEventProcessor<FriendAcceptedEvent> {
    private final NotificationService notificationService;
    private final NotificationRetryService notificationRetryService;

    @Override
    public Class<FriendAcceptedEvent> supports() {
        return FriendAcceptedEvent.class;
    }

    @Override
    public void process(Long outboxId, FriendAcceptedEvent event) {
        try {
            notificationService.createFriendAcceptedNotification(event.requesterId(), event.accepterNickname(), event.friendId());

        } catch (Exception e) {
            saveNotificationRetry(outboxId, event.requesterId(), event, e);
        }

    }

    private void saveNotificationRetry(
            Long outboxId,
            Long receiverId,
            Object event,
            Exception e
    ) {
        try {
            log.error("Notification Retry 저장 outboxId = {}, receiverId = {}, type = {}", outboxId, receiverId, NotificationType.FRIEND_ACCEPTED, e);

            notificationRetryService.save(
                    outboxId,
                    receiverId,
                    NotificationType.FRIEND_ACCEPTED,
                    event,
                    e.getMessage()
            );

        } catch (Exception ex) {
            log.error("친구 수락 알림 Retry 저장 실패", ex);

            throw new BaseException(ErrorEnum.NOTIFICATION_RETRY_SAVE_FAILED);
        }
    }
}
