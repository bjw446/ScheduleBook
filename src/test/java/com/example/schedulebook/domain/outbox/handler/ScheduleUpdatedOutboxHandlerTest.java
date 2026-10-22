package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.schedule.event.ScheduleUpdatedEvent;
import com.example.schedulebook.domain.scheduleshare.service.ScheduleShareUpdateManager;
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
class ScheduleUpdatedOutboxHandlerTest {

    @Mock
    private ScheduleShareUpdateManager scheduleShareUpdateManager;

    private ScheduleUpdatedOutboxHandler scheduleUpdatedOutboxHandler;

    @BeforeEach
    void setUp() {
        scheduleUpdatedOutboxHandler =
                new ScheduleUpdatedOutboxHandler(scheduleShareUpdateManager);
    }

    @Test
    @DisplayName("지원하는 이벤트 타입은 SCHEDULE_UPDATED이다")
    void supports_지원하는이벤트타입을_반환한다() {
        // when
        OutboxEventType result = scheduleUpdatedOutboxHandler.supports();

        // then
        assertThat(result).isEqualTo(OutboxEventType.SCHEDULE_UPDATED);
    }

    @Test
    @DisplayName("Payload 타입은 ScheduleUpdatedEvent이다")
    void payloadType_지원하는Payload타입을_반환한다() {
        // when
        Class<ScheduleUpdatedEvent> result =
                scheduleUpdatedOutboxHandler.payloadType();

        // then
        assertThat(result).isEqualTo(ScheduleUpdatedEvent.class);
    }

    @Test
    @DisplayName("유효한 Payload를 전달하면 일정 ID로 업데이트 처리를 위임한다")
    void handle_유효한Payload이면_일정업데이트처리를_위임한다() {
        // given
        Long outboxId = 1L;
        Long scheduleId = 100L;

        ScheduleUpdatedEvent payload =
                new ScheduleUpdatedEvent("event-123", scheduleId);

        // when
        scheduleUpdatedOutboxHandler.handle(outboxId, payload);

        // then
        verify(scheduleShareUpdateManager).handleUpdated(scheduleId);
    }

    @Test
    @DisplayName("잘못된 Payload 타입을 전달하면 ClassCastException이 발생하고 Manager는 호출되지 않는다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handle_잘못된Payload타입이면_예외가_발생한다() {
        // given
        OutboxEventHandler rawHandler = scheduleUpdatedOutboxHandler;
        Object invalidPayload = new Object();

        // when & then
        assertThatThrownBy(() -> rawHandler.handle(1L, invalidPayload))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(scheduleShareUpdateManager);
    }
}