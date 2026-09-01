package com.example.schedulebook.common.redis.delegate;

import com.example.schedulebook.common.redis.subscriber.RedisSubscriber;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterRetryService;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationRedisMessageDelegateTest {

    private RedisSubscriber redisSubscriber;
    private ObjectMapper objectMapper;
    private DeadLetterRetryService deadLetterRetryService;

    private NotificationRedisMessageDelegate delegate;

    @BeforeEach
    void setUp() {

        redisSubscriber =
                mock(RedisSubscriber.class);

        objectMapper =
                mock(ObjectMapper.class);

        deadLetterRetryService =
                mock(DeadLetterRetryService.class);

        delegate =
                new NotificationRedisMessageDelegate(
                        redisSubscriber,
                        objectMapper,
                        deadLetterRetryService
                );
    }

    @Test
    @DisplayName("정상적인 Redis 메시지를 역직렬화하여 알림 이벤트를 전달한다")
    void givenValidMessage_whenHandleMessage_thenNotifySubscriber() throws Exception {

        // given
        String message =
                "{\"eventId\":\"event-1\",\"receiverId\":1}";

        NotificationEventResponse response =
                mock(NotificationEventResponse.class);

        when(
                objectMapper.readValue(
                        message,
                        NotificationEventResponse.class
                )
        ).thenReturn(
                response
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
                NotificationEventResponse.class
        );

        verify(
                redisSubscriber
        ).onNotification(
                response
        );

        verifyNoInteractions(
                deadLetterRetryService
        );
    }

    @Test
    @DisplayName("손상된 JSON의 역직렬화와 eventId 추출이 모두 실패하면 eventId 없이 Dead Letter를 저장한다")
    void givenInvalidJson_whenHandleMessage_thenSaveDeadLetterWithoutEventId() throws Exception {

        // given
        String message =
                "{\"eventId\":\"event-1\",\"receiverId\":}";

        JsonProcessingException exception =
                mock(JsonProcessingException.class);

        when(
                objectMapper.readValue(
                        message,
                        NotificationEventResponse.class
                )
        ).thenThrow(
                exception
        );

        /*
         * 손상된 JSON이므로 extractEventId 내부의 readTree()도 실패한다.
         */
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
                NotificationEventResponse.class
        );

        verify(
                objectMapper
        ).readTree(
                message
        );

        verify(
                deadLetterRetryService
        ).saveDeadLetterWithRetry(
                eq(DeadLetterType.NOTIFICATION),
                eq(DeadLetterSource.NOTIFICATION_REDIS_MESSAGE_DELEGATE),
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
        /*
         * JSON 자체는 정상이다.
         * 따라서 readTree()는 정상적으로 eventId를 추출할 수 있다.
         */
        String message =
                "{\"eventId\":\"event-123\",\"receiverId\":1}";

        JsonProcessingException exception =
                mock(JsonProcessingException.class);

        JsonNode rootNode =
                mock(JsonNode.class);

        JsonNode eventIdNode =
                mock(JsonNode.class);

        /*
         * 실제 이벤트 역직렬화만 실패시킨다.
         */
        when(
                objectMapper.readValue(
                        message,
                        NotificationEventResponse.class
                )
        ).thenThrow(
                exception
        );

        /*
         * eventId 추출은 성공한다.
         */
        when(
                objectMapper.readTree(
                        message
                )
        ).thenReturn(
                rootNode
        );

        when(
                rootNode.get(
                        "eventId"
                )
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
                NotificationEventResponse.class
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
                eq(DeadLetterType.NOTIFICATION),
                eq(DeadLetterSource.NOTIFICATION_REDIS_MESSAGE_DELEGATE),
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
    @DisplayName("Redis 메시지 처리 중 예상치 못한 예외가 발생해도 예외를 전파하지 않는다")
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
                        NotificationEventResponse.class
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
                NotificationEventResponse.class
        );

        verifyNoInteractions(
                redisSubscriber
        );

        verifyNoInteractions(
                deadLetterRetryService
        );
    }
}