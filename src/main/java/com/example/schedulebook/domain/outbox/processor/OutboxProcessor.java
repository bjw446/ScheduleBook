package com.example.schedulebook.domain.outbox.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.entity.ProcessedOutbox;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus;
import com.example.schedulebook.domain.outbox.handler.OutboxEventHandler;
import com.example.schedulebook.domain.outbox.publisher.OutboxPublisher;
import com.example.schedulebook.domain.outbox.service.OutboxTransactionService;
import com.example.schedulebook.domain.outbox.service.ProcessedOutboxTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final ProcessedOutboxTransactionService processedOutboxTransactionService;

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
                Outbox outbox = outboxTransactionService.findById(outboxId);

                ProcessedOutbox processedOutbox = prepareProcessedOutbox(outboxId, outbox);

                if (processedOutbox == null) {
                    outboxTransactionService.markSuccess(outboxId);

                    continue;
                }

                OutboxEventHandler<?> outboxEventHandler = getHandler(outbox);

                log.info("Outbox 처리 시작 outboxId = {}, eventType = {}", outboxId, outbox.getEventType());

                handleOutbox(outboxEventHandler, outboxId, outbox.getPayload());

                log.info("Outbox Handle 완료 outboxId = {}", outboxId);

                outboxPublisher.publish(outboxId, outbox);

                log.info("Outbox Publisher 완료 outboxId = {}", outboxId);

                completeOutbox(outboxId);

            } catch (Exception e) {
                log.error("Outbox 실패 outboxId = {}", outboxId, e);

                try {
                    processedOutboxTransactionService.markProcessedOutboxFailed(outboxId);

                } catch (Exception ex) {
                    log.error("ProcessedOutbox 상태 변경 실패", ex);
                }

                try {
                    outboxTransactionService.handleFailure(outboxId, e);

                } catch (Exception ex) {
                    log.error("Outbox 실패 처리 실패", ex);
                }
            }
        }
    }

    private <T> void handleOutbox(OutboxEventHandler<T> handler, Long outboxId, String payload) throws Exception {
        log.debug("Outbox Payload 역직렬화 outboxId = {}", outboxId);

        T event = objectMapper.readValue(payload, handler.payloadType());

        handler.handle(outboxId, event);
    }

    private ProcessedOutbox prepareProcessedOutbox(Long outboxId, Outbox outbox) {
        ProcessedOutbox processedOutbox = processedOutboxTransactionService.findByOutboxId(outboxId);

        if (processedOutbox == null) {
            return processedOutboxTransactionService.create(
                    outboxId,
                    outbox.getAggregateType(),
                    outbox.getEventType()
            );
        }

        if (processedOutbox.getStatus() == ProcessedOutboxStatus.SUCCESS) {
            log.debug("이미 처리된 Outbox {}", outboxId);

            return null;
        }

        if (processedOutbox.getStatus() == ProcessedOutboxStatus.FAILED) {
            processedOutboxTransactionService.markProcessedOutboxRetry(outboxId);

            return processedOutboxTransactionService.findByOutboxId(outboxId);
        }

        return processedOutbox;
    }

    private void completeOutbox(Long outboxId) {
        processedOutboxTransactionService.markProcessedOutboxSuccess(outboxId);

        outboxTransactionService.markSuccess(outboxId);

        log.info("Outbox 최종 성공 outboxId = {}", outboxId);
    }

    private OutboxEventHandler<?> getHandler(Outbox outbox) {
        OutboxEventHandler<?> outboxEventHandler = handlerMap.get(outbox.getEventType());

        if (outboxEventHandler == null) {
            log.error("지원하지 않는 이벤트 : {}",outbox.getEventType());

            throw new BaseException(ErrorEnum.INVALID_OUTBOX_EVENT_TYPE);
        }

        return outboxEventHandler;
    }
}
