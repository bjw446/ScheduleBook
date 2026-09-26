package com.example.schedulebook.domain.friend.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.friend.service.FriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendCleanupProcessorTest {

    @Mock
    private FriendService friendService;

    @Mock
    private LoggingExecutor loggingExecutor;

    private FriendCleanupProcessor processor;

    private static final Long OUTBOX_ID = 100L;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        processor = new FriendCleanupProcessor(
                friendService,
                loggingExecutor
        );
    }

    @Test
    void 친구_관계_삭제를_LoggingExecutor에_위임하고_성공하면_true를_반환한다() {
        // given
        ArgumentCaptor<Runnable> runnableCaptor =
                ArgumentCaptor.forClass(Runnable.class);

        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("친구 관계 삭제"),
                runnableCaptor.capture()
        )).thenReturn(true);

        // when
        boolean result = processor.process(OUTBOX_ID, USER_ID);

        // then
        assertThat(result).isTrue();

        verify(loggingExecutor).execute(
                eq(OUTBOX_ID),
                eq("친구 관계 삭제"),
                any(Runnable.class)
        );

        verifyNoInteractions(friendService);

        // LoggingExecutor에 전달된 실제 작업을 실행
        runnableCaptor.getValue().run();

        verify(friendService).removeAllFriendRelations(USER_ID);
    }

    @Test
    void LoggingExecutor가_false를_반환하면_false를_그대로_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("친구 관계 삭제"),
                any(Runnable.class)
        )).thenReturn(false);

        // when
        boolean result = processor.process(OUTBOX_ID, USER_ID);

        // then
        assertThat(result).isFalse();

        verify(loggingExecutor).execute(
                eq(OUTBOX_ID),
                eq("친구 관계 삭제"),
                any(Runnable.class)
        );

        verifyNoInteractions(friendService);
    }
}