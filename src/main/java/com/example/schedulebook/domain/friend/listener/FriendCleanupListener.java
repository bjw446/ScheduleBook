package com.example.schedulebook.domain.friend.listener;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.friend.service.FriendService;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FriendCleanupListener {
    private final FriendService friendService;
    private final LoggingExecutor loggingExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserWithdrawEvent event) {
        loggingExecutor.execute("친구 관계 삭제", () -> friendService.removeAllFriendRelations(event.userId()));
    }
}
