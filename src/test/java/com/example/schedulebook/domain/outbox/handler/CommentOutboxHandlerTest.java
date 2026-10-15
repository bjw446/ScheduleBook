package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.redis.publisher.RedisEventPublisher;
import com.example.schedulebook.domain.comment.dto.response.CommentEventResponse;
import com.example.schedulebook.domain.comment.enums.CommentEventType;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CommentOutboxHandlerTest {

    private static final Long OUTBOX_ID = 1L;

    @Mock
    private RedisEventPublisher redisEventPublisher;

    private CommentOutboxHandler commentOutboxHandler;

    @BeforeEach
    void setUp() {
        commentOutboxHandler = new CommentOutboxHandler(redisEventPublisher);
    }

    @Test
    @DisplayName("지원하는 이벤트 타입으로 COMMENT_EVENT를 반환한다")
    void supports_CommentEvent타입을_반환한다() {
        // given

        // when
        OutboxEventType result = commentOutboxHandler.supports();

        // then
        assertThat(result).isEqualTo(OutboxEventType.COMMENT_EVENT);
    }

    @Test
    @DisplayName("처리할 Payload 타입으로 CommentEventResponse 클래스를 반환한다")
    void payloadType_CommentEventResponse클래스를_반환한다() {
        // given

        // when
        Class<CommentEventResponse> result = commentOutboxHandler.payloadType();

        // then
        assertThat(result).isEqualTo(CommentEventResponse.class);
    }

    @Test
    @DisplayName("유효한 Payload를 전달하면 변환된 이벤트를 지정된 Redis 채널로 발행한다")
    void handle_유효한Payload를_Redis이벤트로_발행한다() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 15, 12, 30);

        CommentEventResponse payload = new CommentEventResponse(
                CommentEventType.CREATED,
                100L,
                "event-123",
                200L,
                50L,
                300L,
                "테스트 사용자",
                "댓글 내용",
                false,
                false,
                createdAt,
                5
        );

        CommentEvent expectedEvent = new CommentEvent(
                CommentEventType.CREATED,
                new CommentEventResponse(
                        CommentEventType.CREATED,
                        100L,
                        "event-123",
                        200L,
                        50L,
                        300L,
                        "테스트 사용자",
                        "댓글 내용",
                        false,
                        false,
                        createdAt,
                        5
                )
        );

        // when
        commentOutboxHandler.handle(OUTBOX_ID, payload);

        // then
        verify(redisEventPublisher).publish(RedisConst.COMMENT, expectedEvent);
    }

    @Test
    @DisplayName("잘못된 Payload 타입을 전달하면 ClassCastException이 발생하고 Redis 이벤트는 발행되지 않는다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handle_잘못된Payload타입이면_예외가_발생한다() {
        // given
        OutboxEventHandler rawHandler = commentOutboxHandler;
        Object invalidPayload = new Object();

        // when & then
        assertThatThrownBy(() -> rawHandler.handle(OUTBOX_ID, invalidPayload))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(redisEventPublisher);
    }
}