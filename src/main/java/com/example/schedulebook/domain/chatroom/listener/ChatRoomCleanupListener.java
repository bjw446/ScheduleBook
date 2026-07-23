package com.example.schedulebook.domain.chatroom.listener;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.chatroom.service.ChatRoomService;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatRoomCleanupListener {
    private final ChatRoomService chatRoomService;
    private final LoggingExecutor loggingExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserWithdrawEvent event) {
        loggingExecutor.execute("채팅방 관계 정리", () -> chatRoomService.removeAllChatRelations(event.userId()));
    }
}
