package com.example.schedulebook.domain.friend.processor;

import com.example.schedulebook.domain.friend.event.FriendRequestedEvent;
import com.example.schedulebook.domain.notification.processor.NotificationEventProcessor;
import com.example.schedulebook.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendRequestProcessor implements NotificationEventProcessor<FriendRequestedEvent> {
    private final NotificationService notificationService;

    @Override
    public Class<FriendRequestedEvent> supports() {
        return FriendRequestedEvent.class;
    }

    @Override
    public void process(FriendRequestedEvent event) {
        notificationService.createFriendRequestNotification(event.receiverId(), event.requesterNickname(), event.friendId());
    }
}
