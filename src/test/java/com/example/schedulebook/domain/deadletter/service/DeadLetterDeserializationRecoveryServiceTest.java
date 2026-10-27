package com.example.schedulebook.domain.deadletter.service;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.publisher.RedisEventPublisher;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.comment.dto.response.CommentEventResponse;
import com.example.schedulebook.domain.comment.enums.CommentEventType;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.notification.enums.NotificationEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterDeserializationRecoveryServiceTest {

    @Mock
    private RedisEventPublisher redisEventPublisher;

    private ObjectMapper objectMapper;

    private DeadLetterDeserializationRecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

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
        ForceLogoutSessionEvent event = new ForceLogoutSessionEvent(
                "event-1",
                1L,
                "session-1",
                1_800_000_000L
        );

        String payload = objectMapper.writeValueAsString(event);

        DeadLetterQueue deadLetterQueue = createDeadLetterQueue();

        when(deadLetterQueue.getDeadLetterType())
                .thenReturn(DeadLetterType.FORCE_LOGOUT);
        when(deadLetterQueue.getPayload())
                .thenReturn(payload);

        // when
        recoveryService.recover(deadLetterQueue);

        // then
        ArgumentCaptor<ForceLogoutSessionEvent> eventCaptor =
                ArgumentCaptor.forClass(ForceLogoutSessionEvent.class);

        verify(redisEventPublisher).publish(
                eq(RedisConst.FORCE_LOGOUT),
                eventCaptor.capture()
        );

        assertThat(eventCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(event);

        verifyNoMoreInteractions(redisEventPublisher);
    }

    @Test
    @DisplayName("댓글 이벤트를 역직렬화하고 Redis 채널에 발행한다")
    void recover_Comment유형이면_이벤트를_역직렬화하고_발행한다()
            throws Exception {
        // given
        CommentEventResponse response = new CommentEventResponse(
                CommentEventType.CREATED,
                10L,
                "event-1",
                100L,
                null,
                1L,
                "테스트 사용자",
                "테스트 댓글입니다.",
                false,
                false,
                LocalDateTime.of(2026, 10, 27, 15, 0, 0),
                1
        );

        CommentEvent event = CommentEvent.from(response);
        String payload = objectMapper.writeValueAsString(event);

        DeadLetterQueue deadLetterQueue = createDeadLetterQueue();

        when(deadLetterQueue.getDeadLetterType())
                .thenReturn(DeadLetterType.COMMENT);
        when(deadLetterQueue.getPayload())
                .thenReturn(payload);

        // when
        recoveryService.recover(deadLetterQueue);

        // then
        ArgumentCaptor<CommentEvent> eventCaptor =
                ArgumentCaptor.forClass(CommentEvent.class);

        verify(redisEventPublisher).publish(
                eq(RedisConst.COMMENT),
                eventCaptor.capture()
        );

        assertThat(eventCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(event);

        verifyNoMoreInteractions(redisEventPublisher);
    }

    @Test
    @DisplayName("알림 이벤트를 역직렬화하고 Redis 채널에 발행한다")
    void recover_Notification유형이면_이벤트를_역직렬화하고_발행한다()
            throws Exception {
        // given
        NotificationEventResponse event = new NotificationEventResponse(
                NotificationEventType.values()[0],
                "event-1",
                1L,
                100L,
                "COMMENT",
                "새로운 댓글",
                "댓글이 등록되었습니다.",
                3L,
                1_800_000_000L
        );

        String payload = objectMapper.writeValueAsString(event);

        DeadLetterQueue deadLetterQueue = createDeadLetterQueue();

        when(deadLetterQueue.getDeadLetterType())
                .thenReturn(DeadLetterType.NOTIFICATION);
        when(deadLetterQueue.getPayload())
                .thenReturn(payload);

        // when
        recoveryService.recover(deadLetterQueue);

        // then
        ArgumentCaptor<NotificationEventResponse> eventCaptor =
                ArgumentCaptor.forClass(NotificationEventResponse.class);

        verify(redisEventPublisher).publish(
                eq(RedisConst.NOTIFICATION),
                eventCaptor.capture()
        );

        assertThat(eventCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(event);

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

        verifyNoMoreInteractions(redisEventPublisher);
    }

    @Test
    @DisplayName("강제 로그아웃 이벤트 역직렬화에 실패하면 JSON 역직렬화 예외로 변환한다")
    void recover_역직렬화에_실패하면_예외를_변환한다() {
        // given
        DeadLetterQueue deadLetterQueue = createDeadLetterQueue();

        when(deadLetterQueue.getId()).thenReturn(1L);
        when(deadLetterQueue.getDeadLetterType())
                .thenReturn(DeadLetterType.FORCE_LOGOUT);
        when(deadLetterQueue.getPayload())
                .thenReturn("invalid-json");

        // when & then
        assertThatThrownBy(() -> recoveryService.recover(deadLetterQueue))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(
                        ErrorEnum.JSON_DESERIALIZATION_FAILED.getMessage()
                );

        verifyNoMoreInteractions(redisEventPublisher);
    }

    @Test
    @DisplayName("강제 로그아웃 이벤트를 JSON으로 직렬화하고 역직렬화할 수 있다")
    void recover_ForceLogout이벤트의_JSON_호환성을_검증한다()
            throws Exception {
        // given
        ForceLogoutSessionEvent originalEvent = new ForceLogoutSessionEvent(
                "event-1",
                1L,
                "session-1",
                1_800_000_000L
        );

        // when
        String payload = objectMapper.writeValueAsString(originalEvent);

        ForceLogoutSessionEvent restoredEvent = objectMapper.readValue(
                payload,
                ForceLogoutSessionEvent.class
        );

        // then
        assertThat(restoredEvent)
                .usingRecursiveComparison()
                .isEqualTo(originalEvent);
    }

    @Test
    @DisplayName("댓글 이벤트를 JSON으로 직렬화하고 역직렬화할 수 있다")
    void recover_Comment이벤트의_JSON_호환성을_검증한다()
            throws Exception {
        // given
        CommentEventResponse response = new CommentEventResponse(
                CommentEventType.CREATED,
                10L,
                "event-1",
                100L,
                null,
                1L,
                "테스트 사용자",
                "테스트 댓글입니다.",
                false,
                false,
                LocalDateTime.of(2026, 10, 27, 15, 0, 0),
                1
        );

        CommentEvent originalEvent = CommentEvent.from(response);

        // when
        String payload = objectMapper.writeValueAsString(originalEvent);

        CommentEvent restoredEvent = objectMapper.readValue(
                payload,
                CommentEvent.class
        );

        // then
        assertThat(restoredEvent)
                .usingRecursiveComparison()
                .isEqualTo(originalEvent);
    }

    @Test
    @DisplayName("알림 이벤트를 JSON으로 직렬화하고 역직렬화할 수 있다")
    void recover_Notification이벤트의_JSON_호환성을_검증한다()
            throws Exception {
        // given
        NotificationEventResponse originalEvent =
                new NotificationEventResponse(
                        NotificationEventType.values()[0],
                        "event-1",
                        1L,
                        100L,
                        "COMMENT",
                        "새로운 댓글",
                        "댓글이 등록되었습니다.",
                        3L,
                        1_800_000_000L
                );

        // when
        String payload = objectMapper.writeValueAsString(originalEvent);

        NotificationEventResponse restoredEvent = objectMapper.readValue(
                payload,
                NotificationEventResponse.class
        );

        // then
        assertThat(restoredEvent)
                .usingRecursiveComparison()
                .isEqualTo(originalEvent);
    }

    private DeadLetterQueue createDeadLetterQueue() {
        return org.mockito.Mockito.mock(DeadLetterQueue.class);
    }
}