package com.example.schedulebook.domain.comment.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentSubscriber {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void onComment(CommentEvent event) {
        try {
            simpMessagingTemplate.convertAndSend(
                    "/topic/schedule/" + event.commentEventResponse().scheduleId() + "/comments",
                    event
            );
        } catch (Exception e) {
            log.error(
                    "댓글 이벤트 전송 실패 scheduleId={}, commentId={}, type={}",
                    event.commentEventResponse().scheduleId(),
                    event.commentEventResponse().id(),
                    event.commentEventResponse().commentEventType(),
                    e
            );
        }
    }
}
