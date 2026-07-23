package com.example.schedulebook.domain.outbox.publisher;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(Outbox outbox) throws JsonProcessingException {
        switch (outbox.getEventType()) {
            case COMMENT_CREATED -> {
                CommentCreatedEvent event = objectMapper.readValue(
                        outbox.getPayload(),
                        CommentCreatedEvent.class
                );

                applicationEventPublisher.publishEvent(event);
            }

            case CHAT_MESSAGE_SENT -> {

            }

            default -> throw new BaseException(ErrorEnum.INVALID_OUTBOX_EVENT_TYPE);
        }
    }
}
