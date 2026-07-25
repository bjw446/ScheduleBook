package com.example.schedulebook.domain.notification.event;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NotificationProcessorRegistry {
    private final Map<Class<?>, NotificationEventProcessor<?>> processorMap;

    public NotificationProcessorRegistry(List<NotificationEventProcessor<?>> list) {
        this.processorMap = list.stream()
                .collect(Collectors.toMap(
                        NotificationEventProcessor::supports,
                        p -> p
                ));
    }

    @SuppressWarnings("unchecked")
    public <T> NotificationEventProcessor<T> get(T event) {
        return (NotificationEventProcessor<T>) processorMap.get(event.getClass());
    }
}
