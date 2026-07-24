package com.example.schedulebook.domain.friend.event;

import com.example.schedulebook.domain.notification.event.NotificationEventMarker;

public record FriendAcceptedEvent(
        Long requesterId,
        String accepterNickname,
        Long friendId
) implements NotificationEventMarker {
}
