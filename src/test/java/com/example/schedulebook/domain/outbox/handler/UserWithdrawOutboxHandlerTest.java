package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import com.example.schedulebook.domain.user.processor.UserWithdrawProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserWithdrawOutboxHandlerTest {

    @Mock
    private UserWithdrawProcessor userWithdrawProcessor;

    private UserWithdrawOutboxHandler userWithdrawOutboxHandler;

    @BeforeEach
    void setUp() {
        userWithdrawOutboxHandler =
                new UserWithdrawOutboxHandler(userWithdrawProcessor);
    }

    @Test
    @DisplayName("지원하는 이벤트 타입은 USER_WITHDRAW이다")
    void supports_지원하는이벤트타입을_반환한다() {
        // when
        OutboxEventType result = userWithdrawOutboxHandler.supports();

        // then
        assertThat(result).isEqualTo(OutboxEventType.USER_WITHDRAW);
    }

    @Test
    @DisplayName("Payload 타입은 UserWithdrawEvent이다")
    void payloadType_지원하는Payload타입을_반환한다() {
        // when
        Class<UserWithdrawEvent> result =
                userWithdrawOutboxHandler.payloadType();

        // then
        assertThat(result).isEqualTo(UserWithdrawEvent.class);
    }

    @Test
    @DisplayName("유효한 Payload를 전달하면 Outbox ID와 Payload로 Processor를 호출한다")
    void handle_유효한Payload이면_Processor에처리를_위임한다() {
        // given
        Long outboxId = 1L;
        UserWithdrawEvent payload = mock(UserWithdrawEvent.class);

        // when
        userWithdrawOutboxHandler.handle(outboxId, payload);

        // then
        verify(userWithdrawProcessor).process(outboxId, payload);
    }

    @Test
    @DisplayName("잘못된 Payload 타입을 전달하면 ClassCastException이 발생하고 Processor는 호출되지 않는다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handle_잘못된Payload타입이면_예외가_발생한다() {
        // given
        OutboxEventHandler rawHandler = userWithdrawOutboxHandler;
        Object invalidPayload = new Object();

        // when & then
        assertThatThrownBy(() -> rawHandler.handle(1L, invalidPayload))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(userWithdrawProcessor);
    }
}