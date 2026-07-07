package com.example.schedulebook.domain.comment.subscriber;

import com.example.schedulebook.common.websocket.WebSocketPublisher;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.example.schedulebook.common.consts.WebSocketDestination.scheduleComment;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentSubscriber {
    private final WebSocketPublisher webSocketPublisher;

    public void onComment(CommentEvent event) {
        webSocketPublisher.send(scheduleComment(event.commentEventResponse().scheduleId()), event);
    }
}
