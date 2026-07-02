package com.example.schedulebook.domain.comment.dto.response;

import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.comment.enums.CommentEventType;

import java.time.LocalDateTime;

public record CommentEventResponse(
        CommentEventType commentEventType,
        Long id,
        Long scheduleId,
        Long parentId,
        Long writerId,
        String writerNickname,
        String content,
        boolean edited,
        boolean deleted,
        LocalDateTime createdAt,
        int commentCount
) {
    public static CommentEventResponse from(Comment comment, CommentEventType commentEventType, int commentCount) {
        return new CommentEventResponse(
                commentEventType,
                comment.getId(),
                comment.getSchedule().getId(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getWriter().getId(),
                comment.getWriter().getNickname(),
                comment.getContent(),
                comment.isEdited(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                commentCount
        );
    }
}
