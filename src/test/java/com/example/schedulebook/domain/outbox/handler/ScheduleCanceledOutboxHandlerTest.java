package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.schedule.event.ScheduleCanceledEvent;
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
class ScheduleCanceledOutboxHandlerTest {

    @Mock
    private ScheduleShareUpdateManager scheduleShareUpdateManager;

    private ScheduleCanceledOutboxHandler scheduleCanceledOutboxHandler;

    @BeforeEach
    void setUp() {
        scheduleCanceledOutboxHandler =
                new ScheduleCanceledOutboxHandler(scheduleShareUpdateManager);
    }

    @Test
    @DisplayName("지원하는 이벤트 타입은 SCHEDULE_CANCELED이다")
    void supports_지원하는이벤트타입을_반환한다() {
        // when
        OutboxEventType result = scheduleCanceledOutboxHandler.supports();

        // then
        assertThat(result).isEqualTo(OutboxEventType.SCHEDULE_CANCELED);
    }

    @Test
    @DisplayName("Payload 타입은 ScheduleCanceledEvent이다")
    void payloadType_지원하는Payload타입을_반환한다() {
        // when
        Class<ScheduleCanceledEvent> result =
                scheduleCanceledOutboxHandler.payloadType();

        // then
        assertThat(result).isEqualTo(ScheduleCanceledEvent.class);
    }

    @Test
    @DisplayName("유효한 Payload를 전달하면 일정 ID와 공유 사용자 ID로 Manager를 호출한다")
    void handle_유효한Payload이면_일정공유업데이트를_처리한다() {
        // given
        Long outboxId = 1L;
        ScheduleCanceledEvent payload = new ScheduleCanceledEvent(
                "event-123",
                100L,
                200L
        );

        // when
        scheduleCanceledOutboxHandler.handle(outboxId, payload);

        // then
        verify(scheduleShareUpdateManager)
                .handleCanceled(payload.scheduleId(), payload.sharedUserId());
    }

    @Test
    @DisplayName("잘못된 Payload 타입을 전달하면 ClassCastException이 발생하고 Manager는 호출되지 않는다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handle_잘못된Payload타입이면_예외가_발생한다() {
        // given
        OutboxEventHandler rawHandler = scheduleCanceledOutboxHandler;
        Object invalidPayload = new Object();

        // when & then
        assertThatThrownBy(() -> rawHandler.handle(1L, invalidPayload))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(scheduleShareUpdateManager);
    }
}