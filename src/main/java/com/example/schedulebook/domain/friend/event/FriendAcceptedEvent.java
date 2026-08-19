package com.example.schedulebook.domain.friend.event;

import com.example.schedulebook.domain.notification.event.NotificationEventMarker;

public record FriendAcceptedEvent(
        String eventId,
        Long requesterId,
        String accepterNickname,
        Long friendId
) implements NotificationEventMarker {
}
