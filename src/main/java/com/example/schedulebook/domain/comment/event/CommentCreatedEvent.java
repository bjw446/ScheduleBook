package com.example.schedulebook.domain.comment.event;

public record CommentCreatedEvent(
        Long scheduleId,
        Long writerId,
        String writerNickname,
        Long parentCommentId
) {
}
