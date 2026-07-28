package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.entity.ProcessedOutbox;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus;
import com.example.schedulebook.domain.outbox.repository.ProcessedOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProcessedOutboxTransactionService {
    private final ProcessedOutboxRepository processedOutboxRepository;

    public ProcessedOutbox findByOutboxId(Long outboxId) {
        return processedOutboxRepository.findByOutboxId(outboxId).orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedOutbox create(Long outboxId, OutboxAggregateType aggregateType, OutboxEventType eventType) {
        return processedOutboxRepository.save(ProcessedOutbox.of(
                outboxId,
                aggregateType,
                eventType
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessedOutboxFailed(Long outboxId) {
        int updated = processedOutboxRepository.markFailed(outboxId);

        if (updated != 1) {
            log.warn("ProcessedOutbox 실패 상태 변경 실패 outboxId = {}", outboxId);

            throw new BaseException(ErrorEnum.PROCESSED_OUTBOX_STATUS_CHANGE_FAILED);
        }

        log.info("ProcessedOutbox 실패 상태 변경 outboxId = {}", outboxId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessedOutboxSuccess(Long outboxId) {
        int updated = processedOutboxRepository.markSuccess(outboxId);

        if (updated != 1) {
            log.warn("ProcessedOutbox 성공 상태 변경 실패 outboxId = {}", outboxId);

            throw new BaseException(ErrorEnum.PROCESSED_OUTBOX_STATUS_CHANGE_FAILED);
        }

        log.info("ProcessedOutbox 성공 상태 변경 outboxId = {}", outboxId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessedOutboxRetry(Long outboxId) {
        int updated = processedOutboxRepository.retry(outboxId);

        if (updated != 1) {
            log.warn("ProcessedOutbox 재시도 상태 변경 실패 outboxId = {}", outboxId);

            throw new BaseException(ErrorEnum.PROCESSED_OUTBOX_STATUS_CHANGE_FAILED);
        }

        log.debug("실패 한 ProcessedOutbox 재시도 {}", outboxId);
    }

    public boolean isAlreadyProcessed(Long outboxId) {
        ProcessedOutbox processedOutbox = processedOutboxRepository.findByOutboxId(outboxId).orElse(null);

        if (processedOutbox == null) {
            return false;
        }

        return processedOutbox.getStatus() == ProcessedOutboxStatus.SUCCESS;
    }
}
