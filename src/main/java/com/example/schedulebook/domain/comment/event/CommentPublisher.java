package com.example.schedulebook.domain.comment.event;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class CommentPublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void publish(CommentEvent event) {
        afterCommit(() -> simpMessagingTemplate.convertAndSend(
                "/topic/schedule/" + event.commentEventResponse().scheduleId() + "/comments",
                event
                ));
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
