package com.example.schedulebook.domain.deadletter.service;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.publisher.RedisEventPublisher;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterDeserializationRecoveryServiceTest {

    @Mock
    private RedisEventPublisher redisEventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    private DeadLetterDeserializationRecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        recoveryService = new DeadLetterDeserializationRecoveryService(
                redisEventPublisher,
                objectMapper
        );
    }

    @Test
    @DisplayName("강제 로그아웃 이벤트를 역직렬화하고 Redis 채널에 발행한다")
    void recover_ForceLogout유형이면_이벤트를_역직렬화하고_발행한다()
            throws Exception {
        // given
        String payload = "{\"sessionId\":\"session-1\"}";

        DeadLetterQueue deadLetterQueue = createDeadLetterQueue();

        when(deadLetterQueue.getDeadLetterType())
                .thenReturn(DeadLetterType.FORCE_LOGOUT);
        when(deadLetterQueue.getPayload())
                .thenReturn(payload);

        ForceLogoutSessionEvent event = org.mockito.Mockito.mock(
                ForceLogoutSessionEvent.class
        );

        when(objectMapper.readValue(
                payload,
                ForceLogoutSessionEvent.class
        )).thenReturn(event);

        // when
        recoveryService.recover(deadLetterQueue);

        // then
        verify(objectMapper).readValue(
                payload,
                ForceLogoutSessionEvent.class
        );

        verify(redisEventPublisher).publish(
                RedisConst.FORCE_LOGOUT,
                event
        );

        verifyNoMoreInteractions(redisEventPublisher);
    }

    @Test
    @DisplayName("댓글 이벤트를 역직렬화하고 Redis 채널에 발행한다")
    void recover_Comment유형이면_이벤트를_역직렬화하고_발행한다()
            throws Exception {
        // given
        String payload = "{\"commentId\":1}";

        DeadLetterQueue deadLetterQueue = createDeadLetterQueue();

        when(deadLetterQueue.getDeadLetterType())
                .thenReturn(DeadLetterType.COMMENT);
        when(deadLetterQueue.getPayload())
                .thenReturn(payload);

        CommentEvent event = org.mockito.Mockito.mock(CommentEvent.class);

        when(objectMapper.readValue(
                payload,
                CommentEvent.class
        )).thenReturn(event);

        // when
        recoveryService.recover(deadLetterQueue);

        // then
        verify(objectMapper).readValue(
                payload,
                CommentEvent.class
        );

        verify(redisEventPublisher).publish(
                RedisConst.COMMENT,
                event
        );

        verifyNoMoreInteractions(redisEventPublisher);
    }

    @Test
    @DisplayName("알림 이벤트를 역직렬화하고 Redis 채널에 발행한다")
    void recover_Notification유형이면_이벤트를_역직렬화하고_발행한다()
            throws Exception {
        // given
        String payload = "{\"notificationId\":1}";

        DeadLetterQueue deadLetterQueue = createDeadLetterQueue();

        when(deadLetterQueue.getDeadLetterType())
                .thenReturn(DeadLetterType.NOTIFICATION);
        when(deadLetterQueue.getPayload())
                .thenReturn(payload);

        NotificationEventResponse event =
                org.mockito.Mockito.mock(NotificationEventResponse.class);

        when(objectMapper.readValue(
                payload,
                NotificationEventResponse.class
        )).thenReturn(event);

        // when
        recoveryService.recover(deadLetterQueue);

        // then
        verify(objectMapper).readValue(
                payload,
                NotificationEventResponse.class
        );

        verify(redisEventPublisher).publish(
                RedisConst.NOTIFICATION,
                event
        );

        verifyNoMoreInteractions(redisEventPublisher);
    }

    @Test
    @DisplayName("지원하지 않는 Dead Letter 유형이면 예외를 발생시킨다")
    void recover_지원하지_않는_유형이면_예외를_발생시킨다() {
        // given
        DeadLetterQueue deadLetterQueue = createDeadLetterQueue();

        when(deadLetterQueue.getDeadLetterType())
                .thenReturn(DeadLetterType.OUTBOX);

        // when & then
        assertThatThrownBy(() -> recoveryService.recover(deadLetterQueue))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(
                        ErrorEnum.INVALID_DEAD_LETTER_TYPE.getMessage()
                );

        verifyNoMoreInteractions(objectMapper);
        verifyNoMoreInteractions(redisEventPublisher);
    }

    @Test
    @DisplayName("강제 로그아웃 이벤트 역직렬화에 실패하면 JSON 역직렬화 예외로 변환한다")
    void recover_역직렬화에_실패하면_예외를_변환한다()
            throws Exception {
        // given
        String payload = "invalid-json";

        DeadLetterQueue deadLetterQueue = createDeadLetterQueue();

        when(deadLetterQueue.getId()).thenReturn(1L);
        when(deadLetterQueue.getDeadLetterType())
                .thenReturn(DeadLetterType.FORCE_LOGOUT);
        when(deadLetterQueue.getPayload())
                .thenReturn(payload);

        JsonProcessingException jsonException =
                new JsonProcessingException("잘못된 JSON 형식") {};

        when(objectMapper.readValue(
                payload,
                ForceLogoutSessionEvent.class
        )).thenThrow(jsonException);

        // when & then
        assertThatThrownBy(() -> recoveryService.recover(deadLetterQueue))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(
                        ErrorEnum.JSON_DESERIALIZATION_FAILED.getMessage()
                );

        verify(objectMapper).readValue(
                payload,
                ForceLogoutSessionEvent.class
        );

        verifyNoMoreInteractions(redisEventPublisher);
    }

    private DeadLetterQueue createDeadLetterQueue() {
        return org.mockito.Mockito.mock(DeadLetterQueue.class);
    }
}