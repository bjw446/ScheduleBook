package com.example.schedulebook.domain.chatroom.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.chatroom.service.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatRoomCleanupProcessorTest {

    private static final Long OUTBOX_ID = 100L;
    private static final Long USER_ID = 1L;

    private ChatRoomService chatRoomService;
    private LoggingExecutor loggingExecutor;
    private ChatRoomCleanupProcessor chatRoomCleanupProcessor;

    @BeforeEach
    void setUp() {
        chatRoomService = mock(ChatRoomService.class);
        loggingExecutor = mock(LoggingExecutor.class);

        chatRoomCleanupProcessor = new ChatRoomCleanupProcessor(
                chatRoomService,
                loggingExecutor
        );
    }

    @Test
    void process_채팅방_관계를_정상적으로_정리하면_true를_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("채팅방 관계 정리"),
                any(Runnable.class)
        )).thenAnswer(invocation -> {
            Runnable cleanupTask = invocation.getArgument(2);
            cleanupTask.run();

            return true;
        });

        // when
        boolean result = chatRoomCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        assertThat(result).isTrue();

        verify(chatRoomService)
                .removeAllChatRelations(USER_ID);

        verify(loggingExecutor)
                .execute(
                        eq(OUTBOX_ID),
                        eq("채팅방 관계 정리"),
                        any(Runnable.class)
                );
    }

    @Test
    void process_LoggingExecutor가_false를_반환하면_false를_반환한다() {
        // given
        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("채팅방 관계 정리"),
                any(Runnable.class)
        )).thenReturn(false);

        // when
        boolean result = chatRoomCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        assertThat(result).isFalse();

        verify(loggingExecutor)
                .execute(
                        eq(OUTBOX_ID),
                        eq("채팅방 관계 정리"),
                        any(Runnable.class)
                );

        verify(chatRoomService, never())
                .removeAllChatRelations(anyLong());
    }

    @Test
    void process_LoggingExecutor가_전달받은_cleanup작업을_실행하면_사용자의_모든_채팅방_관계를_정리한다() {
        // given
        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("채팅방 관계 정리"),
                any(Runnable.class)
        )).thenAnswer(invocation -> {
            Runnable cleanupTask = invocation.getArgument(2);

            cleanupTask.run();

            return true;
        });

        // when
        chatRoomCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        verify(chatRoomService)
                .removeAllChatRelations(USER_ID);
    }

    @Test
    void process_LoggingExecutor가_false를_반환하면_cleanup작업을_실행하지_않는다() {
        // given
        when(loggingExecutor.execute(
                eq(OUTBOX_ID),
                eq("채팅방 관계 정리"),
                any(Runnable.class)
        )).thenReturn(false);

        // when
        chatRoomCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        verify(chatRoomService, never())
                .removeAllChatRelations(anyLong());
    }

    @Test
    void process_채팅방_관계_정리_중_예외가_발생하면_LoggingExecutor가_false를_반환한다() {
        // given
        loggingExecutor = new LoggingExecutor();

        chatRoomCleanupProcessor = new ChatRoomCleanupProcessor(
                chatRoomService,
                loggingExecutor
        );

        doThrow(new RuntimeException("cleanup failed"))
                .when(chatRoomService)
                .removeAllChatRelations(USER_ID);

        // when
        boolean result = chatRoomCleanupProcessor.process(
                OUTBOX_ID,
                USER_ID
        );

        // then
        assertThat(result).isFalse();

        verify(chatRoomService)
                .removeAllChatRelations(USER_ID);
    }
}