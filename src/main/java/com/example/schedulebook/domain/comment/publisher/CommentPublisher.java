package com.example.schedulebook.domain.comment.publisher;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.websocket.WebSocketPublisher;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentPublisher {
    private final WebSocketPublisher webSocketPublisher;

    public void publish(CommentEvent event) {
        webSocketPublisher.sendAfterCommit(
                WebSocketDestination.SCHEDULE_COMMENT(event.commentEventResponse().scheduleId()),
                event
        );
    }
}
