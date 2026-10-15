package com.example.schedulebook.domain.outbox.processor;

import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.entity.ProcessedOutbox;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus;
import com.example.schedulebook.domain.outbox.handler.OutboxEventHandler;
import com.example.schedulebook.domain.outbox.publisher.OutboxPublisher;
import com.example.schedulebook.domain.outbox.service.OutboxTransactionService;
import com.example.schedulebook.domain.outbox.service.ProcessedOutboxTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    private static final Long OUTBOX_ID = 1L;
    private static final String PAYLOAD = "{\"message\":\"test\"}";

    @Mock
    private OutboxPublisher outboxPublisher;

    @Mock
    private OutboxTransactionService outboxTransactionService;

    @Mock
    private OutboxEventHandler<Object> outboxEventHandler;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedOutboxTransactionService processedOutboxTransactionService;

    @Mock
    private Outbox outbox;

    @Mock
    private ProcessedOutbox processedOutbox;

    private OutboxProcessor outboxProcessor;

    private OutboxEventType eventType;

    private Object event;

    @BeforeEach
    void setUp() {
        eventType = OutboxEventType.values()[0];
        event = new Object();

        when(outboxEventHandler.supports()).thenReturn(eventType);

        outboxProcessor = new OutboxProcessor(
                outboxPublisher,
                outboxTransactionService,
                List.of(outboxEventHandler),
                objectMapper,
                processedOutboxTransactionService
        );

        outboxProcessor.init();
    }

    @Test
    @DisplayName("processPendingOutbox는 Outbox를 처리하고 최종 성공 상태로 변경한다")
    void processPendingOutbox_정상적으로_처리하고_성공_상태로_변경한다() throws Exception {
        // given
        when(outboxTransactionService.claimOutboxes())
                .thenReturn(List.of(OUTBOX_ID));

        when(outboxTransactionService.findById(OUTBOX_ID))
                .thenReturn(outbox);

        when(processedOutboxTransactionService.findByOutboxId(OUTBOX_ID))
                .thenReturn(processedOutbox);

        when(processedOutbox.getStatus())
                .thenReturn(ProcessedOutboxStatus.PROCESSING);

        when(outbox.getEventType()).thenReturn(eventType);
        when(outbox.getPayload()).thenReturn(PAYLOAD);

        when(outboxEventHandler.payloadType()).thenReturn(Object.class);

        when(objectMapper.readValue(PAYLOAD, Object.class))
                .thenReturn(event);

        // when
        outboxProcessor.processPendingOutbox();

        // then
        verify(outboxTransactionService).recoverStuck();
        verify(outboxTransactionService).claimOutboxes();
        verify(outboxTransactionService).findById(OUTBOX_ID);

        verify(outboxEventHandler).handle(OUTBOX_ID, event);
        verify(outboxPublisher).publish(OUTBOX_ID, outbox);

        verify(processedOutboxTransactionService)
                .markProcessedOutboxSuccess(OUTBOX_ID);

        verify(outboxTransactionService).markSuccess(OUTBOX_ID);

        verify(processedOutboxTransactionService, never())
                .markProcessedOutboxFailed(OUTBOX_ID);

        verify(outboxTransactionService, never())
                .handleFailure(eq(OUTBOX_ID), any(Exception.class));
    }

    @Test
    @DisplayName("processPendingOutbox는 Payload 역직렬화 실패 시 실패 처리를 요청한다")
    void processPendingOutbox_Payload_역직렬화_실패_시_실패_처리를_요청한다()
            throws Exception {
        // given
        RuntimeException expectedException =
                new RuntimeException("Payload 역직렬화 실패");

        givenProcessingOutbox();

        when(objectMapper.readValue(PAYLOAD, Object.class))
                .thenThrow(expectedException);

        // when
        outboxProcessor.processPendingOutbox();

        // then
        verify(objectMapper).readValue(PAYLOAD, Object.class);

        verify(outboxEventHandler, never())
                .handle(any(), any());

        verify(outboxPublisher, never())
                .publish(OUTBOX_ID, outbox);

        verify(processedOutboxTransactionService)
                .markProcessedOutboxFailed(OUTBOX_ID);

        verify(outboxTransactionService)
                .handleFailure(OUTBOX_ID, expectedException);

        verify(processedOutboxTransactionService, never())
                .markProcessedOutboxSuccess(OUTBOX_ID);

        verify(outboxTransactionService, never())
                .markSuccess(OUTBOX_ID);
    }

    @Test
    @DisplayName("processPendingOutbox는 핸들러 예외 발생 시 실패 처리를 요청한다")
    void processPendingOutbox_핸들러_예외_발생_시_실패_처리를_요청한다()
            throws Exception {
        // given
        RuntimeException expectedException =
                new RuntimeException("이벤트 처리 실패");

        givenProcessingOutbox();

        when(objectMapper.readValue(PAYLOAD, Object.class))
                .thenReturn(event);

        doThrow(expectedException)
                .when(outboxEventHandler)
                .handle(OUTBOX_ID, event);

        // when
        outboxProcessor.processPendingOutbox();

        // then
        verify(outboxEventHandler).handle(OUTBOX_ID, event);

        verify(outboxPublisher, never())
                .publish(OUTBOX_ID, outbox);

        verify(processedOutboxTransactionService)
                .markProcessedOutboxFailed(OUTBOX_ID);

        verify(outboxTransactionService)
                .handleFailure(OUTBOX_ID, expectedException);

        verify(processedOutboxTransactionService, never())
                .markProcessedOutboxSuccess(OUTBOX_ID);

        verify(outboxTransactionService, never())
                .markSuccess(OUTBOX_ID);
    }

    @Test
    @DisplayName("processPendingOutbox는 이미 SUCCESS인 Outbox를 다시 처리하지 않는다")
    void processPendingOutbox_이미_성공한_Outbox는_다시_처리하지_않는다() {
        // given
        when(outboxTransactionService.claimOutboxes())
                .thenReturn(List.of(OUTBOX_ID));

        when(outboxTransactionService.findById(OUTBOX_ID))
                .thenReturn(outbox);

        when(processedOutboxTransactionService.findByOutboxId(OUTBOX_ID))
                .thenReturn(processedOutbox);

        when(processedOutbox.getStatus())
                .thenReturn(ProcessedOutboxStatus.SUCCESS);

        // when
        outboxProcessor.processPendingOutbox();

        // then
        verify(processedOutboxTransactionService)
                .findByOutboxId(OUTBOX_ID);

        verify(outboxTransactionService).markSuccess(OUTBOX_ID);

        verify(outboxEventHandler, never())
                .payloadType();

        verify(outboxEventHandler, never())
                .handle(any(), any());

        verifyNoInteractions(outboxPublisher, objectMapper);

        verify(processedOutboxTransactionService, never())
                .markProcessedOutboxSuccess(OUTBOX_ID);

        verify(processedOutboxTransactionService, never())
                .markProcessedOutboxFailed(OUTBOX_ID);

        verify(outboxTransactionService, never())
                .handleFailure(eq(OUTBOX_ID), any(Exception.class));
    }

    @Test
    @DisplayName("processPendingOutbox는 실패 상태 변경에 실패해도 Outbox 실패 처리를 시도한다")
    void processPendingOutbox_실패_상태_변경_예외에도_Outbox_실패_처리를_시도한다()
            throws Exception {
        // given
        RuntimeException processingException =
                new RuntimeException("이벤트 처리 실패");

        RuntimeException statusException =
                new RuntimeException("ProcessedOutbox 상태 변경 실패");

        givenProcessingOutbox();

        when(objectMapper.readValue(PAYLOAD, Object.class))
                .thenThrow(processingException);

        doThrow(statusException)
                .when(processedOutboxTransactionService)
                .markProcessedOutboxFailed(OUTBOX_ID);

        // when & then
        assertThatCode(() -> outboxProcessor.processPendingOutbox())
                .doesNotThrowAnyException();

        verify(processedOutboxTransactionService)
                .markProcessedOutboxFailed(OUTBOX_ID);

        verify(outboxTransactionService)
                .handleFailure(OUTBOX_ID, processingException);

        verify(processedOutboxTransactionService, never())
                .markProcessedOutboxSuccess(OUTBOX_ID);
    }

    private void givenProcessingOutbox() throws Exception {
        when(outboxTransactionService.claimOutboxes())
                .thenReturn(List.of(OUTBOX_ID));

        when(outboxTransactionService.findById(OUTBOX_ID))
                .thenReturn(outbox);

        when(processedOutboxTransactionService.findByOutboxId(OUTBOX_ID))
                .thenReturn(processedOutbox);

        when(processedOutbox.getStatus())
                .thenReturn(ProcessedOutboxStatus.PROCESSING);

        when(outbox.getEventType()).thenReturn(eventType);
        when(outbox.getPayload()).thenReturn(PAYLOAD);

        when(outboxEventHandler.payloadType()).thenReturn(Object.class);
    }
}