package com.example.schedulebook.domain.outbox.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.entity.ProcessedOutbox;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus;
import com.example.schedulebook.domain.outbox.handler.OutboxEventHandler;
import com.example.schedulebook.domain.outbox.publisher.OutboxPublisher;
import com.example.schedulebook.domain.outbox.repository.ProcessedOutboxRepository;
import com.example.schedulebook.domain.outbox.service.OutboxTransactionService;
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
                Outbox outbox = outboxTransactionService.findById(outboxId);

                ProcessedOutbox processedOutbox = prepareProcessedOutbox(outboxId, outbox);

                if (processedOutbox == null) {
                    outboxTransactionService.markSuccess(outboxId);

                    continue;
                }

                OutboxEventHandler<?> outboxEventHandler = handlerMap.get(outbox.getEventType());

                if (outboxEventHandler == null) {
                    log.error("지원하지 않는 이벤트 : {}",outbox.getEventType());

                    throw new BaseException(ErrorEnum.INVALID_OUTBOX_EVENT_TYPE);
                }

                log.info("Outbox 처리 시작 outboxId = {}, eventType = {}", outboxId, outbox.getEventType());

                handleOutbox(outboxEventHandler, outboxId, outbox.getPayload());

                log.info("Outbox Handle 완료 outboxId = {}", outboxId);

                outboxPublisher.publish(outboxId, outbox);

                log.info("Outbox Publisher 완료 outboxId = {}", outboxId);

                markProcessedOutboxSuccess(outboxId);

                outboxTransactionService.markSuccess(outboxId);

                log.info("Outbox 최종 성공 outboxId = {}", outboxId);

            } catch (Exception e) {
                log.error("Outbox 실패 outboxId = {}", outboxId, e);

                try {
                    markProcessedOutboxFailed(outboxId);

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
        ProcessedOutbox processedOutbox = processedOutboxRepository.findByOutboxId(outboxId).orElse(null);

        if (processedOutbox == null) {
            processedOutbox = processedOutboxRepository.save(ProcessedOutbox.of(
                    outboxId,
                    outbox.getAggregateType(),
                    outbox.getEventType()
            ));

            return processedOutbox;
        }

        if (processedOutbox.getStatus() == ProcessedOutboxStatus.SUCCESS) {
            return null;
        }

        if (processedOutbox.getStatus() == ProcessedOutboxStatus.FAILED) {
            markProcessedOutboxRetry(outboxId);

            return processedOutbox;
        }

        return processedOutbox;
    }

    private void markProcessedOutboxFailed(Long outboxId) {
        int updated = processedOutboxRepository.markFailed(outboxId);

        if (updated != 1) {
            log.warn("ProcessedOutbox 실패 상태 변경 실패 outboxId = {}", outboxId);

            throw new BaseException(ErrorEnum.PROCESSED_OUTBOX_STATUS_CHANGE_FAILED);
        }

        log.info("ProcessedOutbox 실패 상태 변경 outboxId = {}", outboxId);
    }

    private void markProcessedOutboxSuccess(Long outboxId) {
        int updated = processedOutboxRepository.markSuccess(outboxId);

        if (updated != 1) {
            log.warn("ProcessedOutbox 성공 상태 변경 실패 outboxId = {}", outboxId);

            throw new BaseException(ErrorEnum.PROCESSED_OUTBOX_STATUS_CHANGE_FAILED);
        }

        log.info("ProcessedOutbox 성공 상태 변경 outboxId = {}", outboxId);
    }

    private void markProcessedOutboxRetry(Long outboxId) {
        int updated = processedOutboxRepository.retry(outboxId);

        if (updated != 1) {
            log.warn("ProcessedOutbox 재시도 상태 변경 실패 outboxId = {}", outboxId);

            throw new BaseException(ErrorEnum.PROCESSED_OUTBOX_STATUS_CHANGE_FAILED);
        }

        log.debug("실패 한 ProcessedOutbox 재시도 {}", outboxId);
    }
}
