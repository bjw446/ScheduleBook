package com.example.schedulebook.common.redis.publisher;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisEventPublisherTest {

    private StringRedisTemplate stringRedisTemplate;
    private ObjectMapper objectMapper;

    private RedisEventPublisher redisEventPublisher;

    @BeforeEach
    void setUp() {

        stringRedisTemplate =
                mock(StringRedisTemplate.class);

        objectMapper =
                mock(ObjectMapper.class);

        redisEventPublisher =
                new RedisEventPublisher(
                        stringRedisTemplate,
                        objectMapper
                );
    }

    @Test
    @DisplayName("이벤트를 JSON으로 직렬화하여 Redis 채널에 발행한다")
    void givenValidChannelAndEvent_whenPublish_thenSerializeAndSend() throws Exception {

        // given
        String channel =
                "notification";

        Object event =
                new TestEvent(
                        1L,
                        "hello"
                );

        String message =
                "{\"receiverId\":1,\"message\":\"hello\"}";

        when(
                objectMapper.writeValueAsString(
                        event
                )
        ).thenReturn(
                message
        );

        // when
        redisEventPublisher.publish(
                channel,
                event
        );

        // then
        verify(
                objectMapper
        ).writeValueAsString(
                event
        );

        verify(
                stringRedisTemplate
        ).convertAndSend(
                channel,
                message
        );
    }

    @Test
    @DisplayName("channel이 null이면 INVALID_INPUT 예외를 발생시킨다")
    void givenNullChannel_whenPublish_thenThrowInvalidInput() {

        // given
        String channel =
                null;

        Object event =
                new TestEvent(
                        1L,
                        "hello"
                );

        // when & then
        BaseException exception =
                assertThrows(
                        BaseException.class,
                        () -> redisEventPublisher.publish(
                                channel,
                                event
                        )
                );

        assertEquals(
                ErrorEnum.INVALID_INPUT,
                exception.getErrorEnum()
        );

        verifyNoInteractions(
                objectMapper
        );

        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("channel이 빈 문자열이면 INVALID_INPUT 예외를 발생시킨다")
    void givenBlankChannel_whenPublish_thenThrowInvalidInput() {

        // given
        String channel =
                " ";

        Object event =
                new TestEvent(
                        1L,
                        "hello"
                );

        // when & then
        BaseException exception =
                assertThrows(
                        BaseException.class,
                        () -> redisEventPublisher.publish(
                                channel,
                                event
                        )
                );

        assertEquals(
                ErrorEnum.INVALID_INPUT,
                exception.getErrorEnum()
        );

        verifyNoInteractions(
                objectMapper
        );

        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("event가 null이면 INVALID_INPUT 예외를 발생시킨다")
    void givenNullEvent_whenPublish_thenThrowInvalidInput() {

        // given
        String channel =
                "notification";

        Object event =
                null;

        // when & then
        BaseException exception =
                assertThrows(
                        BaseException.class,
                        () -> redisEventPublisher.publish(
                                channel,
                                event
                        )
                );

        assertEquals(
                ErrorEnum.INVALID_INPUT,
                exception.getErrorEnum()
        );

        verifyNoInteractions(
                objectMapper
        );

        verifyNoInteractions(
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("JSON 직렬화에 실패하면 JSON_SERIALIZATION_FAILED 예외를 발생시킨다")
    void givenJsonSerializationFailure_whenPublish_thenThrowSerializationFailed()
            throws Exception {

        // given
        String channel =
                "notification";

        Object event =
                new TestEvent(
                        1L,
                        "hello"
                );

        JsonProcessingException cause =
                new JsonProcessingException(
                        "serialization error"
                ) {
                };

        when(
                objectMapper.writeValueAsString(
                        event
                )
        ).thenThrow(
                cause
        );

        // when & then
        BaseException exception =
                assertThrows(
                        BaseException.class,
                        () -> redisEventPublisher.publish(
                                channel,
                                event
                        )
                );

        assertEquals(
                ErrorEnum.JSON_SERIALIZATION_FAILED,
                exception.getErrorEnum()
        );

        assertSame(
                cause,
                exception.getCause()
        );

        verify(
                objectMapper
        ).writeValueAsString(
                event
        );

        verify(
                stringRedisTemplate,
                never()
        ).convertAndSend(
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Redis 발행 중 일반 예외가 발생하면 예외를 그대로 전파한다")
    void givenRedisPublishFailure_whenPublish_thenPropagateException()
            throws Exception {

        // given
        String channel =
                "notification";

        Object event =
                new TestEvent(
                        1L,
                        "hello"
                );

        String message =
                "{\"receiverId\":1,\"message\":\"hello\"}";

        RuntimeException cause =
                new RuntimeException(
                        "redis error"
                );

        when(
                objectMapper.writeValueAsString(
                        event
                )
        ).thenReturn(
                message
        );

        doThrow(
                cause
        ).when(
                stringRedisTemplate
        ).convertAndSend(
                channel,
                message
        );

        // when & then
        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> redisEventPublisher.publish(
                                channel,
                                event
                        )
                );

        assertSame(
                cause,
                exception
        );

        verify(
                objectMapper
        ).writeValueAsString(
                event
        );

        verify(
                stringRedisTemplate
        ).convertAndSend(
                channel,
                message
        );
    }

    private record TestEvent(
            Long receiverId,
            String message
    ) {
    }
}