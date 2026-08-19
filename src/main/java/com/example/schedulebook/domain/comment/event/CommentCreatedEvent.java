package com.example.schedulebook.domain.comment.event;

import com.example.schedulebook.domain.notification.event.NotificationEventMarker;

public record CommentCreatedEvent(
        String eventId,
        Long scheduleId,
        Long writerId,
        String writerNickname,
        Long parentCommentId
) implements NotificationEventMarker {
}
