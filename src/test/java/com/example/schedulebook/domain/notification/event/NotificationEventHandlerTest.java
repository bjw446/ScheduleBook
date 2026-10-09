package com.example.schedulebook.domain.notification.event;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.notification.processor.NotificationEventProcessor;
import com.example.schedulebook.domain.notification.processor.NotificationProcessorRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

    private static final Long OUTBOX_ID = 1L;

    @Mock
    private NotificationProcessorRegistry notificationProcessorRegistry;

    @Mock
    private NotificationEventProcessor<NotificationEventMarker> notificationEventProcessor;

    @Mock
    private NotificationOutboxEvent notificationOutboxEvent;

    @Mock
    private NotificationEventMarker payload;

    @InjectMocks
    private NotificationEventHandler notificationEventHandler;

    @Test
    void handle_이벤트를_수신하면_해당_Payload의_Processor를_실행한다() {
        // given
        when(notificationOutboxEvent.outboxId()).thenReturn(OUTBOX_ID);
        when(notificationOutboxEvent.payload()).thenReturn(payload);

        when(notificationProcessorRegistry.get(payload))
                .thenReturn(notificationEventProcessor);

        // when
        notificationEventHandler.handle(notificationOutboxEvent);

        // then
        verify(notificationProcessorRegistry).get(payload);

        verify(notificationEventProcessor)
                .process(OUTBOX_ID, payload);
    }

    @Test
    void handle_Processor를_찾지_못하면_NOTIFICATION_EVENT_NOT_FOUND_예외를_발생시킨다() {
        // given
        when(notificationOutboxEvent.payload()).thenReturn(payload);

        when(notificationProcessorRegistry.get(payload))
                .thenReturn(null);

        // when & then
        assertThatThrownBy(() ->
                notificationEventHandler.handle(notificationOutboxEvent)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NOTIFICATION_EVENT_NOT_FOUND);

        verify(notificationProcessorRegistry).get(payload);

        verifyNoInteractions(notificationEventProcessor);
    }

    @Test
    void handle_Processor_실행에_실패하면_발생한_예외를_전달한다() {
        // given
        RuntimeException exception =
                new RuntimeException("알림 이벤트 처리 실패");

        when(notificationOutboxEvent.outboxId()).thenReturn(OUTBOX_ID);
        when(notificationOutboxEvent.payload()).thenReturn(payload);

        when(notificationProcessorRegistry.get(payload))
                .thenReturn(notificationEventProcessor);

        doThrow(exception)
                .when(notificationEventProcessor)
                .process(OUTBOX_ID, payload);

        // when & then
        assertThatThrownBy(() ->
                notificationEventHandler.handle(notificationOutboxEvent)
        )
                .isSameAs(exception);

        verify(notificationProcessorRegistry).get(payload);

        verify(notificationEventProcessor)
                .process(OUTBOX_ID, payload);
    }
}