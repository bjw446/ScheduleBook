package com.example.schedulebook.domain.comment.dto.response;

import com.example.schedulebook.domain.comment.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleCommentResponse(
        Long id,
        Long parentId,
        Long writerId,
        String writerNickname,
        String content,
        boolean edited,
        boolean deleted,
        LocalDateTime createdAt,
        boolean mine,
        List<ScheduleCommentResponse> replies
) {
    public static ScheduleCommentResponse from(
            Comment comment,
            Long currentUserId,
            List<ScheduleCommentResponse> replies
    ) {
        return new ScheduleCommentResponse(
                comment.getId(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getWriter().getId(),
                comment.getWriter().getNickname(),
                comment.getContent(),
                comment.isEdited(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getWriter().getId().equals(currentUserId),
                replies
        );
    }
}
