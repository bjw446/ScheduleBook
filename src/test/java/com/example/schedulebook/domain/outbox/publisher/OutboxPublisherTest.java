package com.example.schedulebook.domain.outbox.publisher;

import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final Long OUTBOX_ID = 1L;
    private static final String PAYLOAD = "{\"message\":\"test\"}";

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private Outbox outbox;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Test
    @DisplayName("publish는 Payload를 이벤트 객체로 변환하여 발행한다")
    void publish_Payload를_이벤트_객체로_변환하여_발행한다() throws Exception {
        // given
        OutboxEventType eventType = OutboxEventType.values()[0];
        Class<?> eventClass = eventType.getEventClass();
        Object event = new Object();

        when(outbox.getEventType()).thenReturn(eventType);
        when(outbox.getPayload()).thenReturn(PAYLOAD);
        when(objectMapper.readValue(
                org.mockito.ArgumentMatchers.eq(PAYLOAD),
                org.mockito.ArgumentMatchers.<Class<Object>>any()
        )).thenReturn(event);

        // when
        outboxPublisher.publish(OUTBOX_ID, outbox);

        // then
        verify(objectMapper).readValue(PAYLOAD, eventClass);
        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    @DisplayName("publish는 Payload 역직렬화 실패 시 예외를 전파한다")
    void publish_Payload_역직렬화_실패_시_예외를_전파한다() throws Exception {
        // given
        OutboxEventType eventType = OutboxEventType.values()[0];
        Class<?> eventClass = eventType.getEventClass();
        RuntimeException expectedException =
                new RuntimeException("Payload 역직렬화 실패");

        when(outbox.getEventType()).thenReturn(eventType);
        when(outbox.getPayload()).thenReturn(PAYLOAD);

        when(objectMapper.readValue(
                org.mockito.ArgumentMatchers.eq(PAYLOAD),
                org.mockito.ArgumentMatchers.<Class<Object>>any()
        )).thenThrow(expectedException);

        // when & then
        assertThatThrownBy(() -> outboxPublisher.publish(OUTBOX_ID, outbox))
                .isSameAs(expectedException);

        verify(applicationEventPublisher, never()).publishEvent(
                org.mockito.ArgumentMatchers.any(Object.class)
        );
    }

    @Test
    @DisplayName("publish는 이벤트 발행 실패 시 예외를 전파한다")
    void publish_이벤트_발행_실패_시_예외를_전파한다() throws Exception {
        // given
        OutboxEventType eventType = OutboxEventType.values()[0];
        Class<?> eventClass = eventType.getEventClass();
        Object event = new Object();

        RuntimeException expectedException =
                new RuntimeException("이벤트 발행 실패");

        when(outbox.getEventType()).thenReturn(eventType);
        when(outbox.getPayload()).thenReturn(PAYLOAD);

        when(objectMapper.readValue(
                org.mockito.ArgumentMatchers.eq(PAYLOAD),
                org.mockito.ArgumentMatchers.<Class<Object>>any()
        )).thenReturn(event);

        doThrow(expectedException)
                .when(applicationEventPublisher)
                .publishEvent(event);

        // when & then
        assertThatThrownBy(() -> outboxPublisher.publish(OUTBOX_ID, outbox))
                .isSameAs(expectedException);

        verify(objectMapper).readValue(PAYLOAD, eventClass);
        verify(applicationEventPublisher).publishEvent(event);
    }
}