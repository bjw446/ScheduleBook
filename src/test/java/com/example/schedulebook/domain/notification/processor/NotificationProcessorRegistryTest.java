package com.example.schedulebook.domain.notification.processor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationProcessorRegistryTest {

    @Test
    void get_이벤트_타입에_맞는_Processor를_반환한다() {
        // given
        TestNotificationEvent event = new TestNotificationEvent();

        NotificationEventProcessor<TestNotificationEvent> processor =
                mock(NotificationEventProcessor.class);

        when(processor.supports())
                .thenReturn(TestNotificationEvent.class);

        NotificationProcessorRegistry registry =
                new NotificationProcessorRegistry(List.of(processor));

        // when
        NotificationEventProcessor<TestNotificationEvent> result =
                registry.get(event);

        // then
        assertThat(result)
                .isSameAs(processor);
    }

    @Test
    void get_지원하지_않는_이벤트_타입이면_null을_반환한다() {
        // given
        NotificationEventProcessor<TestNotificationEvent> processor =
                mock(NotificationEventProcessor.class);

        when(processor.supports())
                .thenReturn(TestNotificationEvent.class);

        NotificationProcessorRegistry registry =
                new NotificationProcessorRegistry(List.of(processor));

        UnsupportedNotificationEvent event =
                new UnsupportedNotificationEvent();

        // when
        NotificationEventProcessor<UnsupportedNotificationEvent> result =
                registry.get(event);

        // then
        assertThat(result)
                .isNull();
    }

    @Test
    void get_여러_Processor가_등록되어_있으면_이벤트_타입에_맞는_Processor를_선택한다() {
        // given
        TestNotificationEvent event =
                new TestNotificationEvent();

        AnotherNotificationEvent anotherEvent =
                new AnotherNotificationEvent();

        NotificationEventProcessor<TestNotificationEvent> processor =
                mock(NotificationEventProcessor.class);

        NotificationEventProcessor<AnotherNotificationEvent> anotherProcessor =
                mock(NotificationEventProcessor.class);

        when(processor.supports())
                .thenReturn(TestNotificationEvent.class);

        when(anotherProcessor.supports())
                .thenReturn(AnotherNotificationEvent.class);

        NotificationProcessorRegistry registry =
                new NotificationProcessorRegistry(
                        List.of(processor, anotherProcessor)
                );

        // when
        NotificationEventProcessor<TestNotificationEvent> result =
                registry.get(event);

        NotificationEventProcessor<AnotherNotificationEvent> anotherResult =
                registry.get(anotherEvent);

        // then
        assertThat(result)
                .isSameAs(processor);

        assertThat(anotherResult)
                .isSameAs(anotherProcessor);
    }

    private static class TestNotificationEvent {
    }

    private static class AnotherNotificationEvent {
    }

    private static class UnsupportedNotificationEvent {
    }
}