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

    public void process(Long userId) {
        loggingExecutor.execute("댓글 정리", () -> commentService.removeAllComments(userId));
    }
}
