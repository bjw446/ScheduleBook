package com.example.schedulebook.domain.comment.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentValidator {
    private final CommentRepository commentRepository;

    public Comment validateComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new BaseException(ErrorEnum.COMMENT_NOT_FOUND)
        );

        if (comment.isDeleted()) {
            throw new BaseException(ErrorEnum.COMMENT_ALREADY_DELETE);
        }

        return comment;
    }

    public Comment validateParentComment(Long scheduleId, Long parentId) {
        Comment parent = validateComment(parentId);

        if (!parent.getSchedule().getId().equals(scheduleId)) {
            throw new BaseException(ErrorEnum.COMMENT_FORBIDDEN);
        }

        if (parent.getParent() != null) {
            throw new BaseException(ErrorEnum.INVALID_COMMENT);
        }

        return parent;
    }

    public void validateCommentWriter(Comment comment, Long currentUserId) {
        if (!comment.getWriter().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.COMMENT_FORBIDDEN);
        }
    }
}
