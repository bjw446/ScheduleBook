package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.comment.processor.CommentCreatedProcessor;
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
class CommentCreatedOutboxHandlerTest {

    private static final Long OUTBOX_ID = 1L;

    @Mock
    private CommentCreatedProcessor commentCreatedProcessor;

    private CommentCreatedOutboxHandler commentCreatedOutboxHandler;

    @BeforeEach
    void setUp() {
        commentCreatedOutboxHandler =
                new CommentCreatedOutboxHandler(commentCreatedProcessor);
    }

    @Test
    @DisplayName("지원하는 이벤트 타입으로 COMMENT_CREATED를 반환한다")
    void supports_CommentCreated이벤트타입을_반환한다() {
        // given

        // when
        OutboxEventType result = commentCreatedOutboxHandler.supports();

        // then
        assertThat(result).isEqualTo(OutboxEventType.COMMENT_CREATED);
    }

    @Test
    @DisplayName("처리할 Payload 타입으로 CommentCreatedEvent 클래스를 반환한다")
    void payloadType_CommentCreatedEvent클래스를_반환한다() {
        // given

        // when
        Class<CommentCreatedEvent> result =
                commentCreatedOutboxHandler.payloadType();

        // then
        assertThat(result).isEqualTo(CommentCreatedEvent.class);
    }

    @Test
    @DisplayName("유효한 Payload를 전달하면 CommentCreatedProcessor에 정확한 인자를 전달한다")
    void handle_유효한Payload를_Processor에_전달한다() {
        // given
        CommentCreatedEvent payload =
                org.mockito.Mockito.mock(CommentCreatedEvent.class);

        // when
        commentCreatedOutboxHandler.handle(OUTBOX_ID, payload);

        // then
        verify(commentCreatedProcessor).process(OUTBOX_ID, payload);
    }

    @Test
    @DisplayName("잘못된 Payload 타입을 전달하면 ClassCastException이 발생하고 Processor는 호출되지 않는다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handle_잘못된Payload타입이면_예외가_발생한다() {
        // given
        OutboxEventHandler rawHandler = commentCreatedOutboxHandler;
        Object invalidPayload = new Object();

        // when & then
        assertThatThrownBy(() -> rawHandler.handle(OUTBOX_ID, invalidPayload))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(commentCreatedProcessor);
    }
}