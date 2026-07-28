package com.example.schedulebook.domain.chatroom.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.chatroom.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomCleanupProcessor {
    private final ChatRoomService chatRoomService;
    private final LoggingExecutor loggingExecutor;

    public boolean process(Long outboxId, Long userId) {
        return loggingExecutor.execute(
                outboxId,
                "채팅방 관계 정리",
                () -> chatRoomService.removeAllChatRelations(userId)
        );
    }
}
