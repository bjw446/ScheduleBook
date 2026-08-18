package com.example.schedulebook.domain.friend.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.event.FriendRequestedEvent;
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
public class FriendRequestProcessor implements NotificationEventProcessor<FriendRequestedEvent> {
    private final NotificationService notificationService;
    private final NotificationRetryService notificationRetryService;

    @Override
    public Class<FriendRequestedEvent> supports() {
        return FriendRequestedEvent.class;
    }

    @Override
    public void process(Long outboxId, FriendRequestedEvent event) {
        try {
            notificationService.createFriendRequestNotification(event.receiverId(), event.requesterNickname(), event.friendId());

        } catch (Exception e) {
            saveNotificationRetry(outboxId, event, e);
        }

    }

    private void saveNotificationRetry(
            Long outboxId,
            FriendRequestedEvent event,
            Exception e
    ) {
        try {
            log.error(
                    "Notification Retry 저장 outboxId = {}, receiverId = {}, type = {}",
                    outboxId,
                    event.receiverId(),
                    NotificationType.FRIEND_REQUEST,
                    e
            );

            notificationRetryService.save(
                    event.eventId(),
                    outboxId,
                    event.receiverId(),
                    NotificationType.FRIEND_REQUEST,
                    event,
                    e.getMessage()
            );

        } catch (Exception ex) {
            log.error("친구 요청 알림 Retry 저장 실패", ex);

            throw new BaseException(ErrorEnum.NOTIFICATION_RETRY_SAVE_FAILED, ex);
        }
    }
}
