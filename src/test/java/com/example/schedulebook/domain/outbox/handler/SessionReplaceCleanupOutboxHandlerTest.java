package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.auth.event.ReplaceSessionCleanupEvent;
import com.example.schedulebook.domain.auth.service.SessionService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionReplaceCleanupOutboxHandlerTest {

    @Mock
    private SessionService sessionService;

    private SessionReplaceCleanupOutboxHandler sessionReplaceCleanupOutboxHandler;

    @BeforeEach
    void setUp() {
        sessionReplaceCleanupOutboxHandler =
                new SessionReplaceCleanupOutboxHandler(sessionService);
    }

    @Test
    @DisplayName("지원하는 이벤트 타입은 SESSION_REPLACE_CLEANUP이다")
    void supports_지원하는이벤트타입을_반환한다() {
        // when
        OutboxEventType result = sessionReplaceCleanupOutboxHandler.supports();

        // then
        assertThat(result).isEqualTo(OutboxEventType.SESSION_REPLACE_CLEANUP);
    }

    @Test
    @DisplayName("Payload 타입은 ReplaceSessionCleanupEvent이다")
    void payloadType_지원하는Payload타입을_반환한다() {
        // when
        Class<ReplaceSessionCleanupEvent> result =
                sessionReplaceCleanupOutboxHandler.payloadType();

        // then
        assertThat(result).isEqualTo(ReplaceSessionCleanupEvent.class);
    }

    @Test
    @DisplayName("유효한 Payload를 전달하면 기존 세션 ID로 정리 처리를 위임한다")
    void handle_유효한Payload이면_기존세션정리를_위임한다() {
        // given
        Long outboxId = 1L;
        String oldSessionId = "old-session-123";

        ReplaceSessionCleanupEvent payload = mock(ReplaceSessionCleanupEvent.class);
        when(payload.oldSessionId()).thenReturn(oldSessionId);

        // when
        sessionReplaceCleanupOutboxHandler.handle(outboxId, payload);

        // then
        verify(sessionService).cleanupReplacedSession(oldSessionId);
    }

    @Test
    @DisplayName("잘못된 Payload 타입을 전달하면 ClassCastException이 발생하고 SessionService는 호출되지 않는다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handle_잘못된Payload타입이면_예외가_발생한다() {
        // given
        OutboxEventHandler rawHandler = sessionReplaceCleanupOutboxHandler;
        Object invalidPayload = new Object();

        // when & then
        assertThatThrownBy(() -> rawHandler.handle(1L, invalidPayload))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(sessionService);
    }
}