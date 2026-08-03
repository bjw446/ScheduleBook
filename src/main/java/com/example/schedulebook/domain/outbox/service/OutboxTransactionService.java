package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import com.example.schedulebook.domain.outbox.dto.RetryDecision;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;
import com.example.schedulebook.domain.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxTransactionService {
    private final OutboxRepository outboxRepository;
    private final OutboxRetryPolicy outboxRetryPolicy;
    private final DeadLetterService deadLetterService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> claimOutboxes() {
        List<Outbox> outboxes = outboxRepository.findRetryTargets(LocalDateTime.now(), CommonConst.BATCH_SIZE);

        outboxes.forEach(Outbox::processing);

        outboxRepository.flush();

        return outboxes.stream().map(Outbox::getId).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long outboxId) {
        int updated = outboxRepository.updateStatusIfProcessing(outboxId, OutboxStatus.SUCCESS, LocalDateTime.now());

        if (updated == 0) {
            log.warn("Outbox {} 상태 전이 실패 : 이미 다른 트랜잭션에서 처리됨", outboxId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(Long outboxId, Exception e) {
        Outbox outbox = findById(outboxId);

        int nextRetry = outbox.getRetryCount() + 1;

        RetryDecision retryDecision = outboxRetryPolicy.decide(nextRetry);

        if (retryDecision.isDead()) {
            log.error("Outbox {} 영구 실패", outbox.getId(), e);

            try {
                deadLetterSave(
                        getDeadLetterAggregateId(outbox),
                        outbox.getPayload(),
                        normalizeMessage(e),
                        e.getClass().getSimpleName(),
                        nextRetry
                );

            } catch (Exception exception) {
                log.error("DLQ 저장 실패", exception);

                return;
            }

        } else {
            log.warn("Outbox {} 발행 실패 : {}회 재시도 예정", outbox.getId(), nextRetry, e);
        }

        int updated = outboxRepository.updateFailureIfProcessing(
                outboxId,
                retryDecision.outboxStatus(),
                normalizeMessage(e),
                retryDecision.nextRetryAt()
        );

        if (updated == 0) {
            log.warn("Outbox {} 실패 처리 건너뜀 : 이미 다른 트랜잭션에서 상태 변경됨", outboxId);

        } else {
            log.debug("Outbox {} 상태 {}로 변경", outboxId, retryDecision.outboxStatus());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverStuck() {
        List<Outbox> outboxes = outboxRepository.findStuckOutboxes(
                LocalDateTime.now().minusMinutes(30),
                CommonConst.BATCH_SIZE
        );

        for (Outbox outbox : outboxes) {
            int nextRetry = outbox.getRetryCount() + 1;

            RetryDecision retryDecision = outboxRetryPolicy.decide(nextRetry);

            String errorMessage;

            if (retryDecision.isDead()) {
                errorMessage = "Outbox lease timeout으로 DEAD 처리";

                log.error("Outbox {} lease timeout으로 DEAD 처리", outbox.getId());

                try {
                    deadLetterSave(
                            getDeadLetterAggregateId(outbox),
                            outbox.getPayload(),
                            errorMessage,
                            "LEASE_TIMEOUT",
                            nextRetry
                    );

                } catch (Exception e) {
                    log.error("DLQ 저장 실패", e);

                    continue;
                }

            } else {
                errorMessage = "Outbox 처리 중 시간 초과";

                log.warn("Outbox {} 처리 중 시간 초과", outbox.getId());
            }

            int updated = outboxRepository.updateRecoverIfProcessing(
                    outbox.getId(),
                    retryDecision.outboxStatus(),
                    errorMessage,
                    retryDecision.nextRetryAt()
            );

            recoverLog(outbox.getId(), retryDecision.outboxStatus(), updated, false);
        }
    }

    @Transactional
    public void recover(Long outboxId) {
        Outbox outbox = findById(outboxId);

        int updated = outboxRepository.updateRecover(outbox.getId());

        recoverLog(outboxId, OutboxStatus.PENDING, updated, true);
    }

    public Outbox findById(Long outboxId) {
        return outboxRepository.findById(outboxId).orElseThrow(
                () -> new BaseException(ErrorEnum.OUTBOX_NOT_FOUND)
        );
    }

    private String normalizeMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) {
            return "Unknown Error";
        }

        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private void deadLetterSave(
            String aggregateId,
            String payload,
            String reason,
            String exceptionType,
            int retryCount
    ) {
        deadLetterService.save(
                DeadLetterType.OUTBOX,
                DeadLetterSource.OUTBOX_TRANSACTION_SERVICE,
                DeadLetterAggregateType.OUTBOX,
                aggregateId,
                null,
                payload,
                reason,
                exceptionType,
                retryCount
        );
    }

    private String getDeadLetterAggregateId(Outbox outbox) {
        return outbox.getId() == null ? null : outbox.getId().toString();
    }

    private void recoverLog(Long outboxId, OutboxStatus outboxStatus, int updated, boolean throwOnFail) {
        if (updated == 0) {
            log.warn("Outbox {} 복구 건너뜀 : 이미 다른 트랜잭션에서 상태 변경됨", outboxId);

            if (throwOnFail) {
                throw new BaseException(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);
            }
        } else {
            log.debug("Outbox {} 상태 {}로 복구 성공", outboxId, outboxStatus);
        }
    }
}
