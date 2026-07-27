package com.example.schedulebook.domain.outbox.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.entity.ProcessedOutbox;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.handler.OutboxEventHandler;
import com.example.schedulebook.domain.outbox.publisher.OutboxPublisher;
import com.example.schedulebook.domain.outbox.repository.ProcessedOutboxRepository;
import com.example.schedulebook.domain.outbox.service.OutboxTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private final OutboxPublisher outboxPublisher;
    private final OutboxTransactionService outboxTransactionService;
    private final List<OutboxEventHandler<?>> handlers;
    private final Map<OutboxEventType, OutboxEventHandler<?>> handlerMap = new EnumMap<>(OutboxEventType.class);
    private final ObjectMapper objectMapper;
    private final ProcessedOutboxRepository processedOutboxRepository;

    @PostConstruct
    void init() {
        handlers.forEach(handler ->
                handlerMap.put(
                        handler.supports(),
                        handler
                ));
    }

    public void processPendingOutbox() {
        outboxTransactionService.recoverStuck();

        List<Long> outboxIds = outboxTransactionService.claimOutboxes();

        for (Long outboxId : outboxIds) {
            try {
                if (processedOutboxRepository.existsByOutboxId(outboxId)) {
                    outboxTransactionService.markSuccess(outboxId);

                    continue;
                }

                Outbox outbox = outboxTransactionService.findById(outboxId);

                OutboxEventHandler<?> outboxEventHandler = handlerMap.get(outbox.getEventType());

                if (outboxEventHandler == null) {
                    log.error("지원하지 않는 이벤트 : {}",outbox.getEventType());

                    throw new BaseException(ErrorEnum.INVALID_OUTBOX_EVENT_TYPE);
                }

                handleOutbox(outboxEventHandler, outboxId, outbox.getPayload());

                outboxPublisher.publish(outbox);

                processedOutboxRepository.save(ProcessedOutbox.of(
                        outboxId,
                        outbox.getAggregateType(),
                        outbox.getEventType(),
                        LocalDateTime.now()
                        ));

                outboxTransactionService.markSuccess(outboxId);

            } catch (Exception e) {
                outboxTransactionService.handleFailure(outboxId, e);
            }
        }
    }

    private <T> void handleOutbox(OutboxEventHandler<T> handler, Long outboxId, String payload) throws Exception {
        T event = objectMapper.readValue(payload, handler.payloadType());

        handler.handle(outboxId, event);
    }
}
