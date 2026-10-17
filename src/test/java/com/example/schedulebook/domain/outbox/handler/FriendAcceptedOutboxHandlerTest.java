package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.processor.FriendAcceptedProcessor;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
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
class FriendAcceptedOutboxHandlerTest {

    private static final Long OUTBOX_ID = 1L;

    @Mock
    private FriendAcceptedProcessor friendAcceptedProcessor;

    private FriendAcceptedOutboxHandler friendAcceptedOutboxHandler;

    @BeforeEach
    void setUp() {
        friendAcceptedOutboxHandler =
                new FriendAcceptedOutboxHandler(friendAcceptedProcessor);
    }

    @Test
    @DisplayName("지원하는 이벤트 타입으로 FRIEND_ACCEPTED를 반환한다")
    void supports_FriendAccepted이벤트타입을_반환한다() {
        // given

        // when
        OutboxEventType result = friendAcceptedOutboxHandler.supports();

        // then
        assertThat(result).isEqualTo(OutboxEventType.FRIEND_ACCEPTED);
    }

    @Test
    @DisplayName("처리할 Payload 타입으로 FriendAcceptedEvent 클래스를 반환한다")
    void payloadType_FriendAcceptedEvent클래스를_반환한다() {
        // given

        // when
        Class<FriendAcceptedEvent> result =
                friendAcceptedOutboxHandler.payloadType();

        // then
        assertThat(result).isEqualTo(FriendAcceptedEvent.class);
    }

    @Test
    @DisplayName("유효한 Payload를 전달하면 FriendAcceptedProcessor에 정확한 인자를 전달한다")
    void handle_유효한Payload를_Processor에_전달한다() {
        // given
        FriendAcceptedEvent payload = mock(FriendAcceptedEvent.class);

        // when
        friendAcceptedOutboxHandler.handle(OUTBOX_ID, payload);

        // then
        verify(friendAcceptedProcessor).process(OUTBOX_ID, payload);
    }

    @Test
    @DisplayName("잘못된 Payload 타입을 전달하면 ClassCastException이 발생하고 Processor는 호출되지 않는다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handle_잘못된Payload타입이면_예외가_발생한다() {
        // given
        OutboxEventHandler rawHandler = friendAcceptedOutboxHandler;
        Object invalidPayload = new Object();

        // when & then
        assertThatThrownBy(() -> rawHandler.handle(OUTBOX_ID, invalidPayload))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(friendAcceptedProcessor);
    }
}