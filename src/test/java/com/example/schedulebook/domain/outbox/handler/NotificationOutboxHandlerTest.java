package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.redis.publisher.RedisEventPublisher;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
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
class NotificationOutboxHandlerTest {

    private static final Long OUTBOX_ID = 1L;

    @Mock
    private RedisEventPublisher redisEventPublisher;

    private NotificationOutboxHandler notificationOutboxHandler;

    @BeforeEach
    void setUp() {
        notificationOutboxHandler =
                new NotificationOutboxHandler(redisEventPublisher);
    }

    @Test
    @DisplayName("지원하는 이벤트 타입으로 NOTIFICATION_EVENT를 반환한다")
    void supports_NotificationEvent타입을_반환한다() {
        // given

        // when
        OutboxEventType result = notificationOutboxHandler.supports();

        // then
        assertThat(result).isEqualTo(OutboxEventType.NOTIFICATION_EVENT);
    }

    @Test
    @DisplayName("처리할 Payload 타입으로 NotificationEventResponse 클래스를 반환한다")
    void payloadType_NotificationEventResponse클래스를_반환한다() {
        // given

        // when
        Class<NotificationEventResponse> result =
                notificationOutboxHandler.payloadType();

        // then
        assertThat(result).isEqualTo(NotificationEventResponse.class);
    }

    @Test
    @DisplayName("유효한 Payload를 전달하면 지정된 Redis 채널로 발행한다")
    void handle_유효한Payload를_Redis채널로_발행한다() {
        // given
        NotificationEventResponse payload =
                mock(NotificationEventResponse.class);

        // when
        notificationOutboxHandler.handle(OUTBOX_ID, payload);

        // then
        verify(redisEventPublisher).publish(RedisConst.NOTIFICATION, payload);
    }

    @Test
    @DisplayName("잘못된 Payload 타입을 전달하면 ClassCastException이 발생하고 Redis 이벤트는 발행되지 않는다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handle_잘못된Payload타입이면_예외가_발생한다() {
        // given
        OutboxEventHandler rawHandler = notificationOutboxHandler;
        Object invalidPayload = new Object();

        // when & then
        assertThatThrownBy(() -> rawHandler.handle(OUTBOX_ID, invalidPayload))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(redisEventPublisher);
    }
}