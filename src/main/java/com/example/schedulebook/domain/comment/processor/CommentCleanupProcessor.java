package com.example.schedulebook.domain.comment.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentCleanupProcessor {
    private final CommentService commentService;
    private final LoggingExecutor loggingExecutor;

    public boolean process(Long outboxId, Long userId) {
        return loggingExecutor.execute(
                outboxId,
                "댓글 정리",
                () -> commentService.removeAllComments(userId)
        );
    }
}
