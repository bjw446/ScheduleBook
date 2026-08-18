package com.example.schedulebook.domain.friend.event;

import com.example.schedulebook.domain.notification.event.NotificationEventMarker;

public record FriendRequestedEvent(
        String eventId,
        Long receiverId,
        String requesterNickname,
        Long friendId
) implements NotificationEventMarker {
}
