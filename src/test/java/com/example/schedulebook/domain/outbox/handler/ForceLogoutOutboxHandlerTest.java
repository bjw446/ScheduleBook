package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.redis.publisher.RedisEventPublisher;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ForceLogoutOutboxHandlerTest {

    private static final Long OUTBOX_ID = 1L;

    @Mock
    private RedisEventPublisher redisEventPublisher;

    private ForceLogoutOutboxHandler forceLogoutOutboxHandler;

    @BeforeEach
    void setUp() {
        forceLogoutOutboxHandler =
                new ForceLogoutOutboxHandler(redisEventPublisher);
    }

    @Test
    @DisplayName("지원하는 이벤트 타입으로 FORCE_LOGOUT을 반환한다")
    void supports_ForceLogout이벤트타입을_반환한다() {
        // given

        // when
        OutboxEventType result = forceLogoutOutboxHandler.supports();

        // then
        assertThat(result).isEqualTo(OutboxEventType.FORCE_LOGOUT);
    }

    @Test
    @DisplayName("처리할 Payload 타입으로 ForceLogoutSessionEvent 클래스를 반환한다")
    void payloadType_ForceLogoutSessionEvent클래스를_반환한다() {
        // given

        // when
        Class<ForceLogoutSessionEvent> result =
                forceLogoutOutboxHandler.payloadType();

        // then
        assertThat(result).isEqualTo(ForceLogoutSessionEvent.class);
    }

    @Test
    @DisplayName("유효한 Payload를 전달하면 지정된 Redis 채널로 이벤트를 발행한다")
    void handle_유효한Payload를_Redis채널로_발행한다() {
        // given
        ForceLogoutSessionEvent payload = new ForceLogoutSessionEvent(
                "event-123",
                100L,
                "session-456",
                1_800_000_000L
        );

        ForceLogoutSessionEvent expectedEvent = new ForceLogoutSessionEvent(
                "event-123",
                100L,
                "session-456",
                1_800_000_000L
        );

        // when
        forceLogoutOutboxHandler.handle(OUTBOX_ID, payload);

        // then
        verify(redisEventPublisher).publish(RedisConst.FORCE_LOGOUT, expectedEvent);
    }

    @Test
    @DisplayName("잘못된 Payload 타입을 전달하면 ClassCastException이 발생하고 Redis 이벤트는 발행되지 않는다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handle_잘못된Payload타입이면_예외가_발생한다() {
        // given
        OutboxEventHandler rawHandler = forceLogoutOutboxHandler;
        Object invalidPayload = new Object();

        // when & then
        assertThatThrownBy(() -> rawHandler.handle(OUTBOX_ID, invalidPayload))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(redisEventPublisher);
    }
}