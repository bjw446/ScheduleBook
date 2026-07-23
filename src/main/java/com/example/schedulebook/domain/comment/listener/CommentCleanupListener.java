package com.example.schedulebook.domain.comment.listener;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.comment.service.CommentService;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommentCleanupListener {
    private final CommentService commentService;
    private final LoggingExecutor loggingExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserWithdrawEvent event) {
        loggingExecutor.execute("댓글 정리", () -> commentService.removeAllComments(event.userId()));
    }
}
