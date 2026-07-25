package com.example.schedulebook.domain.notification.event;

public interface NotificationEventProcessor<T> {
    Class<T> supports();

    void process(T event);
}
