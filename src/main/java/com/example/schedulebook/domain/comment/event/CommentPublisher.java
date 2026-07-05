package com.example.schedulebook.domain.comment.event;

import com.example.schedulebook.common.executor.AfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentPublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AfterCommitExecutor afterCommitExecutor;

    public void publish(CommentEvent event) {
        afterCommitExecutor.execute(() -> {
            try {
                simpMessagingTemplate.convertAndSend(
                        "/topic/schedule/" + event.commentEventResponse().scheduleId() + "/comments",
                        event
                );
            } catch (Exception e) {
                log.error("커밋 후 이벤트 발행 실패", e);
            }
        });
    }
}
