package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.comment.processor.CommentCreatedProcessor;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentCreatedOutboxHandler implements OutboxEventHandler<CommentCreatedEvent> {
    private final CommentCreatedProcessor commentCreatedProcessor;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.COMMENT_CREATED;
    }

    @Override
    public Class<CommentCreatedEvent> payloadType() {
        return CommentCreatedEvent.class;
    }

    @Override
    public void handle(Long outboxId, CommentCreatedEvent payload) {
        commentCreatedProcessor.process(outboxId, payload);
    }
}
