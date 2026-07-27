package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.comment.dto.response.CommentEventResponse;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import com.example.schedulebook.domain.comment.publisher.CommentPublisher;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentOutboxHandler implements OutboxEventHandler<CommentEventResponse> {
    private final CommentPublisher commentPublisher;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.COMMENT_EVENT;
    }

    @Override
    public Class<CommentEventResponse> payloadType() {
        return CommentEventResponse.class;
    }

    @Override
    public void handle(Long outboxId, CommentEventResponse payload) {
        commentPublisher.publish(CommentEvent.from(payload));
    }
}
