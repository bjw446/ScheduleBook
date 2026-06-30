package com.example.schedulebook.domain.comment.dto.response;

import com.example.schedulebook.domain.comment.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleCommentResponse(
        Long id,
        Long writerId,
        String writerNickname,
        String profileImage,
        String content,
        boolean edited,
        boolean deleted,
        LocalDateTime createAt,
        boolean mine,
        List<ScheduleCommentResponse> replies
) {
    public static ScheduleCommentResponse from(
            Comment comment,
            Long currentUserId,
            String profileImage,
            List<ScheduleCommentResponse> replies
    ) {
        return new ScheduleCommentResponse(
                comment.getId(),
                comment.getWriter().getId(),
                comment.getWriter().getNickname(),
                profileImage,
                comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent(),
                comment.isEdited(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getWriter().getId().equals(currentUserId),
                replies
        );
    }
}
