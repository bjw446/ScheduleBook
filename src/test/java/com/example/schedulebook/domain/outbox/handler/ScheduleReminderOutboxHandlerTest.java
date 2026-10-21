package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.schedule.event.ScheduleReminderEvent;
import com.example.schedulebook.domain.schedule.processor.ScheduleReminderProcessor;
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
class ScheduleReminderOutboxHandlerTest {

    @Mock
    private ScheduleReminderProcessor scheduleReminderProcessor;

    private ScheduleReminderOutboxHandler scheduleReminderOutboxHandler;

    @BeforeEach
    void setUp() {
        scheduleReminderOutboxHandler =
                new ScheduleReminderOutboxHandler(scheduleReminderProcessor);
    }

    @Test
    @DisplayName("지원하는 이벤트 타입은 SCHEDULE_REMINDER이다")
    void supports_지원하는이벤트타입을_반환한다() {
        // when
        OutboxEventType result = scheduleReminderOutboxHandler.supports();

        // then
        assertThat(result).isEqualTo(OutboxEventType.SCHEDULE_REMINDER);
    }

    @Test
    @DisplayName("Payload 타입은 ScheduleReminderEvent이다")
    void payloadType_지원하는Payload타입을_반환한다() {
        // when
        Class<ScheduleReminderEvent> result =
                scheduleReminderOutboxHandler.payloadType();

        // then
        assertThat(result).isEqualTo(ScheduleReminderEvent.class);
    }

    @Test
    @DisplayName("유효한 Payload를 전달하면 Outbox ID와 Payload로 Processor를 호출한다")
    void handle_유효한Payload이면_Processor에처리를_위임한다() {
        // given
        Long outboxId = 1L;
        ScheduleReminderEvent payload = org.mockito.Mockito.mock(
                ScheduleReminderEvent.class
        );

        // when
        scheduleReminderOutboxHandler.handle(outboxId, payload);

        // then
        verify(scheduleReminderProcessor).process(outboxId, payload);
    }

    @Test
    @DisplayName("잘못된 Payload 타입을 전달하면 ClassCastException이 발생하고 Processor는 호출되지 않는다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handle_잘못된Payload타입이면_예외가_발생한다() {
        // given
        OutboxEventHandler rawHandler = scheduleReminderOutboxHandler;
        Object invalidPayload = new Object();

        // when & then
        assertThatThrownBy(() -> rawHandler.handle(1L, invalidPayload))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(scheduleReminderProcessor);
    }
}