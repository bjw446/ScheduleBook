package com.example.schedulebook.domain.friend.processor;

import com.example.schedulebook.common.executor.LoggingExecutor;
import com.example.schedulebook.domain.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendCleanupProcessor {
    private final FriendService friendService;
    private final LoggingExecutor loggingExecutor;

    public void process(Long outboxId, Long userId) {
        loggingExecutor.execute("친구 관계 삭제", () -> friendService.removeAllFriendRelations(userId));
    }
}
