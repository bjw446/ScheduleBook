package com.example.schedulebook.domain.friend.processor;

import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.notification.processor.NotificationEventProcessor;
import com.example.schedulebook.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendAcceptedProcessor implements NotificationEventProcessor<FriendAcceptedEvent> {
    private final NotificationService notificationService;

    @Override
    public Class<FriendAcceptedEvent> supports() {
        return FriendAcceptedEvent.class;
    }

    @Override
    public void process(Long outboxId, FriendAcceptedEvent event) {
        notificationService.createFriendAcceptedNotification(event.requesterId(), event.accepterNickname(), event.friendId());
    }
}
