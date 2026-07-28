package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import com.example.schedulebook.domain.user.processor.UserWithdrawProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserWithdrawOutboxHandler implements OutboxEventHandler<UserWithdrawEvent> {
    private final UserWithdrawProcessor userWithdrawProcessor;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.USER_WITHDRAW;
    }

    @Override
    public Class<UserWithdrawEvent> payloadType() {
        return UserWithdrawEvent.class;
    }

    @Override
    public void handle(Long outboxId, UserWithdrawEvent payload) {
        userWithdrawProcessor.process(outboxId, payload);
    }
}
