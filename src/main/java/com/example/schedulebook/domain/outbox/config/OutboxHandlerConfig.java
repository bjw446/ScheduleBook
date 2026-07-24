package com.example.schedulebook.domain.outbox.config;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.event.OutboxEventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class OutboxHandlerConfig {
    @Bean
    public Map<OutboxEventType, OutboxEventHandler> outboxHandlers(List<OutboxEventHandler> handlers) {
        return handlers.stream()
                .collect(Collectors.toMap(
                        OutboxEventHandler::supports,
                        Function.identity()
                ));
    }
}
