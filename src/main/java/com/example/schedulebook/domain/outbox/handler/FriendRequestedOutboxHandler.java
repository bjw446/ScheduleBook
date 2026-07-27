package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.friend.event.FriendRequestedEvent;
import com.example.schedulebook.domain.friend.processor.FriendRequestProcessor;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendRequestedOutboxHandler implements OutboxEventHandler<FriendRequestedEvent> {
    private final FriendRequestProcessor friendRequestProcessor;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.FRIEND_REQUESTED;
    }

    @Override
    public Class<FriendRequestedEvent> payloadType() {
        return FriendRequestedEvent.class;
    }

    @Override
    public void handle(Long outboxId, FriendRequestedEvent payload) {
        friendRequestProcessor.process(outboxId, payload);
    }
}
