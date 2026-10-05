package com.example.schedulebook.domain.comment.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.comment.service.CommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentCleanupProcessorTest {

    private static final Long OUTBOX_ID = 100L;
    private static final Long USER_ID = 1L;

    @Mock
    private CommentService commentService;

    @Mock
    private LoggingExecutor loggingExecutor;

    @InjectMocks
    private CommentCleanupProcessor commentCleanupProcessor;

    @Test
    void process_댓글_전체_삭제_작업을_실행하고_성공하면_true를_반환한다() {
        // given
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        }).when(loggingExecutor)
                .execute(
                        eq(OUTBOX_ID),
                        eq("댓글 정리"),
                        any(Runnable.class)
                );

        // when
        boolean result = commentCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        assertThat(result)
                .isTrue();

        verify(commentService)
                .removeAllComments(USER_ID);

        verify(loggingExecutor)
                .execute(
                        eq(OUTBOX_ID),
                        eq("댓글 정리"),
                        any(Runnable.class)
                );
    }

    @Test
    void process_댓글_정리_실행_결과가_false이면_false를_반환한다() {
        // given
        doThrow(new RuntimeException("댓글 삭제 실패"))
                .when(commentService)
                .removeAllComments(USER_ID);

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);

            try {
                action.run();
                return true;
            } catch (Exception e) {
                return false;
            }
        }).when(loggingExecutor)
                .execute(
                        eq(OUTBOX_ID),
                        eq("댓글 정리"),
                        any(Runnable.class)
                );

        // when
        boolean result = commentCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        assertThat(result)
                .isFalse();

        verify(commentService)
                .removeAllComments(USER_ID);

        verify(loggingExecutor)
                .execute(
                        eq(OUTBOX_ID),
                        eq("댓글 정리"),
                        any(Runnable.class)
                );
    }
}