package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.domain.friend.event.FriendRequestedEvent;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationPublishService;
import com.example.schedulebook.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendRequestProcessor implements NotificationEventProcessor<FriendRequestedEvent>{
    private final NotificationService notificationService;
    private final NotificationPublishService notificationPublishService;

    @Override
    public Class<FriendRequestedEvent> supports() {
        return FriendRequestedEvent.class;
    }

    @Override
    public void process(FriendRequestedEvent event) {
        notificationService.createFriendRequestNotification(event.receiverId(), event.requesterNickname(), event.friendId());

        notificationPublishService.publish(event.receiverId(), NotificationType.FRIEND_REQUEST, event.requesterNickname());
    }
}
