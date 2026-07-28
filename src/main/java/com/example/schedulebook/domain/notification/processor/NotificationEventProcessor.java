package com.example.schedulebook.domain.notification.processor;

public interface NotificationEventProcessor<T> {
    Class<T> supports();

    void process(Long outboxId, T event);
}
