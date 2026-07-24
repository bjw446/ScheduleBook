package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationPublishService;
import com.example.schedulebook.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendAcceptedProcessor implements NotificationEventProcessor<FriendAcceptedEvent> {
    private final NotificationService notificationService;
    private final NotificationPublishService notificationPublishService;

    @Override
    public Class<FriendAcceptedEvent> supports() {
        return FriendAcceptedEvent.class;
    }

    @Override
    public void process(FriendAcceptedEvent event) {
        notificationService.createFriendAcceptedNotification(event.requesterId(), event.accepterNickname(), event.friendId());

        notificationPublishService.publish(event.requesterId(), NotificationType.FRIEND_ACCEPTED, event.accepterNickname());
    }
}
