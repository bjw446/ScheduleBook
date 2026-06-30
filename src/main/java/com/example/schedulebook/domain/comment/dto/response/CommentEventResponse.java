package com.example.schedulebook.domain.comment.dto.response;

import com.example.schedulebook.domain.comment.entity.Comment;

import java.time.LocalDateTime;

public record CommentEventResponse(
        Long id,
        Long scheduleId,
        Long parentId,
        Long writerId,
        String writerNickname,
        String content,
        boolean edited,
        boolean deleted,
        LocalDateTime createdAt
) {
    public static CommentEventResponse from(Comment comment) {
        return new CommentEventResponse(
                comment.getId(),
                comment.getSchedule().getId(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getWriter().getId(),
                comment.getWriter().getNickname(),
                comment.getContent(),
                comment.isEdited(),
                comment.isDeleted(),
                comment.getCreatedAt()
        );
    }
}
