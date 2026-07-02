package com.example.schedulebook.domain.comment.event;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentSubscriber {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void onComment(CommentEvent event) {
        simpMessagingTemplate.convertAndSend(
                "/topic/schedule/" + event.commentEventResponse().scheduleId() + "/comments",
                event
        );
    }
}
