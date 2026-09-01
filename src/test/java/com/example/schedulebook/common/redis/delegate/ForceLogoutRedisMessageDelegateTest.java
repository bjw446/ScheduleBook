package com.example.schedulebook.common.redis.delegate;

import com.example.schedulebook.common.redis.subscriber.RedisSubscriber;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterRetryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ForceLogoutRedisMessageDelegateTest {

    private RedisSubscriber redisSubscriber;
    private ObjectMapper objectMapper;
    private DeadLetterRetryService deadLetterRetryService;

    private ForceLogoutRedisMessageDelegate delegate;

    @BeforeEach
    void setUp() {

        redisSubscriber =
                mock(RedisSubscriber.class);

        objectMapper =
                mock(ObjectMapper.class);

        deadLetterRetryService =
                mock(DeadLetterRetryService.class);

        delegate =
                new ForceLogoutRedisMessageDelegate(
                        redisSubscriber,
                        objectMapper,
                        deadLetterRetryService
                );
    }

    @Test
    @DisplayName("정상적인 Redis 메시지를 역직렬화하여 강제 로그아웃 이벤트를 전달한다")
    void givenValidMessage_whenHandleMessage_thenForceLogoutSubscriber() throws Exception {

        // given
        String message =
                "{\"eventId\":\"event-1\",\"sessionId\":\"session-1\"}";

        ForceLogoutSessionEvent event =
                mock(ForceLogoutSessionEvent.class);

        when(
                objectMapper.readValue(
                        message,
                        ForceLogoutSessionEvent.class
                )
        ).thenReturn(
                event
        );

        // when
        delegate.handleMessage(
                message
        );

        // then
        verify(
                objectMapper
        ).readValue(
                message,
                ForceLogoutSessionEvent.class
        );

        verify(
                redisSubscriber
        ).onForceLogout(
                event
        );

        verifyNoInteractions(
                deadLetterRetryService
        );
    }

    @Test
    @DisplayName("잘못된 JSON이면 강제 로그아웃 메시지를 처리하지 않는다")
    void givenInvalidJson_whenHandleMessage_thenNotNotifySubscriber() throws Exception {

        // given
        String message =
                "{\"eventId\":\"event-1\",\"sessionId\":}";

        JsonProcessingException exception =
                mock(JsonProcessingException.class);

        when(
                objectMapper.readValue(
                        message,
                        ForceLogoutSessionEvent.class
                )
        ).thenThrow(
                exception
        );

        when(
                objectMapper.readTree(
                        message
                )
        ).thenThrow(
                exception
        );

        // when
        delegate.handleMessage(
                message
        );

        // then
        verify(
                objectMapper
        ).readValue(
                message,
                ForceLogoutSessionEvent.class
        );

        verify(
                objectMapper
        ).readTree(
                message
        );

        verify(
                deadLetterRetryService
        ).saveDeadLetterWithRetry(
                eq(DeadLetterType.FORCE_LOGOUT),
                eq(DeadLetterSource.FORCE_LOGOUT_REDIS_MESSAGE_DELEGATE),
                eq(DeadLetterAggregateType.DESERIALIZATION_ERROR),
                isNull(),
                isNull(),
                eq(message),
                eq(exception),
                isNull()
        );

        verifyNoInteractions(
                redisSubscriber
        );
    }

    @Test
    @DisplayName("역직렬화 실패 시 eventId를 추출하여 Dead Letter에 전달한다")
    void givenDeserializationFailure_whenHandleMessage_thenExtractEventId() throws Exception {

        // given
        String message =
                "{\"eventId\":\"event-123\",\"sessionId\":\"session-1\"}";

        JsonProcessingException exception =
                mock(JsonProcessingException.class);

        JsonNode rootNode =
                mock(JsonNode.class);

        JsonNode eventIdNode =
                mock(JsonNode.class);

        when(
                objectMapper.readValue(
                        message,
                        ForceLogoutSessionEvent.class
                )
        ).thenThrow(
                exception
        );

        when(
                objectMapper.readTree(
                        message
                )
        ).thenReturn(
                rootNode
        );

        when(
                rootNode.get("eventId")
        ).thenReturn(
                eventIdNode
        );

        when(
                eventIdNode.asText()
        ).thenReturn(
                "event-123"
        );

        // when
        delegate.handleMessage(
                message
        );

        // then
        verify(
                objectMapper
        ).readValue(
                message,
                ForceLogoutSessionEvent.class
        );

        verify(
                objectMapper
        ).readTree(
                message
        );

        verify(
                rootNode
        ).get(
                "eventId"
        );

        verify(
                eventIdNode
        ).asText();

        verify(
                deadLetterRetryService
        ).saveDeadLetterWithRetry(
                eq(DeadLetterType.FORCE_LOGOUT),
                eq(DeadLetterSource.FORCE_LOGOUT_REDIS_MESSAGE_DELEGATE),
                eq(DeadLetterAggregateType.DESERIALIZATION_ERROR),
                isNull(),
                isNull(),
                eq(message),
                eq(exception),
                eq("event-123")
        );

        verifyNoInteractions(
                redisSubscriber
        );
    }

    @Test
    @DisplayName("예상치 못한 예외가 발생해도 예외를 전파하지 않는다")
    void givenUnexpectedException_whenHandleMessage_thenNotPropagateException() throws Exception {

        // given
        String message =
                "{\"eventId\":\"event-1\"}";

        RuntimeException exception =
                new RuntimeException(
                        "unexpected error"
                );

        when(
                objectMapper.readValue(
                        message,
                        ForceLogoutSessionEvent.class
                )
        ).thenThrow(
                exception
        );

        // when & then
        assertDoesNotThrow(
                () -> delegate.handleMessage(
                        message
                )
        );

        verify(
                objectMapper
        ).readValue(
                message,
                ForceLogoutSessionEvent.class
        );

        verifyNoInteractions(
                redisSubscriber
        );

        verifyNoInteractions(
                deadLetterRetryService
        );
    }
}