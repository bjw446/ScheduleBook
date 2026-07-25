package com.example.schedulebook.domain.comment.event;

import com.example.schedulebook.domain.notification.event.NotificationEventMarker;

public record CommentCreatedEvent(
        Long scheduleId,
        Long writerId,
        String writerNickname,
        Long parentCommentId
) implements NotificationEventMarker {
}
