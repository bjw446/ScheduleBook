package com.example.schedulebook.common.redis.event;

public record RedisEvent<T>(
        String eventId,
        T payload
) {
}
