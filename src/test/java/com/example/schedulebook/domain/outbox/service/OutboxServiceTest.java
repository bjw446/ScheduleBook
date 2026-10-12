package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    private static final String EVENT_ID = "event-123";
    private static final String AGGREGATE_ID = "schedule-456";
    private static final String JSON_PAYLOAD = "{\"scheduleId\":456}";

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    @DisplayName("정상적인 payload를 JSON으로 직렬화하여 Outbox를 저장한다")
    void save_정상적인_payload를_JSON으로_직렬화하여_Outbox를_저장한다()
            throws JsonProcessingException {
        // given
        OutboxAggregateType aggregateType = OutboxAggregateType.SCHEDULE;
        OutboxEventType eventType = OutboxEventType.SCHEDULE_UPDATED;
        Object payload = new Object();

        when(objectMapper.writeValueAsString(payload))
                .thenReturn(JSON_PAYLOAD);

        // when
        outboxService.save(
                EVENT_ID,
                aggregateType,
                AGGREGATE_ID,
                eventType,
                payload
        );

        // then
        ArgumentCaptor<Outbox> outboxCaptor =
                ArgumentCaptor.forClass(Outbox.class);

        verify(objectMapper).writeValueAsString(payload);
        verify(outboxRepository).save(outboxCaptor.capture());

        Outbox savedOutbox = outboxCaptor.getValue();

        assertThat(savedOutbox.getEventId()).isEqualTo(EVENT_ID);
        assertThat(savedOutbox.getAggregateType()).isEqualTo(aggregateType);
        assertThat(savedOutbox.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(savedOutbox.getEventType()).isEqualTo(eventType);
        assertThat(savedOutbox.getPayload()).isEqualTo(JSON_PAYLOAD);

        verifyNoMoreInteractions(outboxRepository);
    }

    @Test
    @DisplayName("동일한 eventId를 전달하면 해당 eventId를 Outbox에 설정한다")
    void save_동일한_eventId를_전달하면_해당_eventId를_Outbox에_설정한다()
            throws JsonProcessingException {
        // given
        OutboxAggregateType aggregateType = OutboxAggregateType.SCHEDULE;
        OutboxEventType eventType = OutboxEventType.SCHEDULE_UPDATED;
        Object payload = new Object();

        when(objectMapper.writeValueAsString(payload))
                .thenReturn(JSON_PAYLOAD);

        // when
        outboxService.save(
                EVENT_ID,
                aggregateType,
                AGGREGATE_ID,
                eventType,
                payload
        );

        // then
        ArgumentCaptor<Outbox> outboxCaptor =
                ArgumentCaptor.forClass(Outbox.class);

        verify(outboxRepository).save(outboxCaptor.capture());

        assertThat(outboxCaptor.getValue().getEventId())
                .isEqualTo(EVENT_ID);

        verifyNoMoreInteractions(outboxRepository);
    }

    @Test
    @DisplayName("JSON 직렬화에 실패하면 직렬화 실패 예외가 발생한다")
    void save_JSON_직렬화에_실패하면_직렬화_실패_예외가_발생한다()
            throws JsonProcessingException {
        // given
        OutboxAggregateType aggregateType = OutboxAggregateType.SCHEDULE;
        OutboxEventType eventType = OutboxEventType.SCHEDULE_UPDATED;
        Object payload = new Object();

        JsonProcessingException cause =
                new JsonProcessingException("JSON serialization failed") {};

        when(objectMapper.writeValueAsString(payload))
                .thenThrow(cause);

        // when & then
        assertThatThrownBy(() -> outboxService.save(
                EVENT_ID,
                aggregateType,
                AGGREGATE_ID,
                eventType,
                payload
        ))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;

                    assertThat(baseException.getErrorEnum())
                            .isEqualTo(
                                    ErrorEnum.OUTBOX_PAYLOAD_SERIALIZATION_FAILED
                            );
                })
                .hasCause(cause);

        verify(objectMapper).writeValueAsString(payload);
        verifyNoInteractions(outboxRepository);
    }

    @Test
    @DisplayName("Pending 이벤트 취소 시 집계 유형, 집계 ID, 이벤트 유형을 전달한다")
    void cancelPending_지정한_조건으로_Pending_이벤트_상태_변경을_위임한다() {
        // given
        OutboxAggregateType aggregateType = OutboxAggregateType.SCHEDULE;
        String aggregateId = AGGREGATE_ID;
        OutboxEventType eventType = OutboxEventType.SCHEDULE_UPDATED;

        // when
        outboxService.cancelPending(
                aggregateType,
                aggregateId,
                eventType
        );

        // then
        verify(outboxRepository).updateStatusToDeadIfPending(
                aggregateType,
                aggregateId,
                eventType
        );

        verifyNoMoreInteractions(outboxRepository);
    }
}