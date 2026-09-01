package com.example.schedulebook.common.redis.delegate;

import com.example.schedulebook.common.redis.subscriber.RedisSubscriber;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterRetryService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
                new ObjectMapper();

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
    @DisplayName("정상적인 Redis 메시지를 실제 역직렬화하여 강제 로그아웃 이벤트를 전달한다")
    void givenValidMessage_whenHandleMessage_thenForceLogoutSubscriber() throws Exception {

        // given
        String message =
                """
                {
                    "eventId": "event-1",
                    "sessionId": "session-1"
                }
                """;

        // when
        delegate.handleMessage(
                message
        );

        // then
        verify(
                redisSubscriber
        ).onForceLogout(
                argThat(event ->
                        event != null
                                && "event-1".equals(event.eventId())
                                && "session-1".equals(event.sessionId())
                )
        );

        verifyNoInteractions(
                deadLetterRetryService
        );
    }

    @Test
    @DisplayName("잘못된 JSON이면 강제 로그아웃 이벤트를 전달하지 않고 Dead Letter를 저장한다")
    void givenInvalidJson_whenHandleMessage_thenSaveDeadLetter() {

        // given
        String message =
                """
                {
                    "eventId": "event-1",
                    "sessionId":
                }
                """;

        // when
        delegate.handleMessage(
                message
        );

        // then
        verifyNoInteractions(
                redisSubscriber
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
                any(JsonProcessingException.class),
                isNull()
        );
    }

    @Test
    @DisplayName("역직렬화에 실패해도 유효한 JSON이면 실제 eventId를 추출하여 Dead Letter에 전달한다")
    void givenDeserializationFailure_whenHandleMessage_thenExtractEventId() throws Exception {

        // given
        String message =
                """
                {
                    "eventId": "event-123",
                    "sessionId": "session-1"
                }
                """;

        ForceLogoutRedisMessageDelegate delegate =
                new ForceLogoutRedisMessageDelegate(
                        redisSubscriber,
                        new ObjectMapper() {
                            @Override
                            public <T> T readValue(
                                    String content,
                                    Class<T> valueType
                            ) throws JsonProcessingException {
                                throw new JsonProcessingException(
                                        "forced deserialization failure"
                                ) {
                                };
                            }
                        },
                        deadLetterRetryService
                );

        // when
        delegate.handleMessage(
                message
        );

        // then
        verify(
                deadLetterRetryService
        ).saveDeadLetterWithRetry(
                eq(DeadLetterType.FORCE_LOGOUT),
                eq(DeadLetterSource.FORCE_LOGOUT_REDIS_MESSAGE_DELEGATE),
                eq(DeadLetterAggregateType.DESERIALIZATION_ERROR),
                isNull(),
                isNull(),
                eq(message),
                any(JsonProcessingException.class),
                eq("event-123")
        );

        verifyNoInteractions(
                redisSubscriber
        );
    }

    @Test
    @DisplayName("JSON 파싱 자체가 실패하면 eventId를 추출하지 못하고 null을 전달한다")
    void givenMalformedJson_whenHandleMessage_thenEventIdIsNull() {

        // given
        String message =
                """
                {
                    "eventId": "event-1",
                    "sessionId":
                """;

        // when
        delegate.handleMessage(
                message
        );

        // then
        verifyNoInteractions(
                redisSubscriber
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
                any(JsonProcessingException.class),
                isNull()
        );
    }

    @Test
    @DisplayName("예상치 못한 예외가 발생해도 예외를 전파하지 않는다")
    void givenUnexpectedException_whenHandleMessage_thenNotPropagateException()
            throws Exception {

        // given
        String message =
                "{\"eventId\":\"event-1\"}";

        ObjectMapper objectMapper =
                mock(ObjectMapper.class);

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

        delegate =
                new ForceLogoutRedisMessageDelegate(
                        redisSubscriber,
                        objectMapper,
                        deadLetterRetryService
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