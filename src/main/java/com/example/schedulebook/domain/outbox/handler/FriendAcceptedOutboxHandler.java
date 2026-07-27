package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.processor.FriendAcceptedProcessor;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendAcceptedOutboxHandler implements OutboxEventHandler<FriendAcceptedEvent> {
    private final FriendAcceptedProcessor friendAcceptedProcessor;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.FRIEND_ACCEPTED;
    }

    @Override
    public Class<FriendAcceptedEvent> payloadType() {
        return FriendAcceptedEvent.class;
    }

    @Override
    public void handle(FriendAcceptedEvent payload) {
        friendAcceptedProcessor.process(payload);
    }
}
