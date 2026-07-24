package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
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
            log.info("Outbox {} 상태 전이 실패 : 이미 다른 트랜잭션에서 처리됨", outboxId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(Long outboxId, Exception e) {
        Outbox outbox = findById(outboxId);

        outbox.increaseRetryCount();

        OutboxStatus outboxStatus;

        LocalDateTime nextRetryAt = null;

        if (outbox.getRetryCount() >= CommonConst.MAX_RETRY) {
            outboxStatus = OutboxStatus.DEAD;

            log.error("Outbox {} 영구 실패", outbox.getId(), e);

        } else {
            outboxStatus = OutboxStatus.FAILED;

            nextRetryAt = LocalDateTime.now().plusMinutes(CommonConst.OUTBOX_RETRY_DELAY);

            log.error("Outbox 발행 실패 : {}", outbox.getId(), e);
        }

        int updated = outboxRepository.updateFailureIfProcessing(
                outboxId,
                outboxStatus,
                normalizeMessage(e),
                nextRetryAt
        );

        if (updated == 0) {
            log.info("Outbox {} 실패 처리 건너뜀 : 이미 다른 트랜잭션에서 상태 변경됨", outboxId);

        } else {
            log.error("Outbox {} 발행 실패 처리", outboxId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverStuck() {
        List<Outbox> outboxes = outboxRepository.findStuckOutboxes(
                LocalDateTime.now().minusMinutes(30),
                CommonConst.BATCH_SIZE
        );

        for (Outbox outbox : outboxes) {
            outbox.increaseRetryCount();

            OutboxStatus outboxStatus;

            LocalDateTime nextRetryAt = null;

            String errorMessage;

            if (outbox.getRetryCount() >= CommonConst.MAX_RETRY) {
                outboxStatus = OutboxStatus.DEAD;

                errorMessage = "Outbox lease timeout으로 DEAD 처리";

                log.error("Outbox {} lease timeout으로 DEAD 처리", outbox.getId());

            } else {
                outboxStatus = OutboxStatus.FAILED;

                errorMessage = "Outbox 처리 중 시간 초과";

                nextRetryAt = LocalDateTime.now().plusMinutes(CommonConst.OUTBOX_RETRY_DELAY);

                log.error("Outbox {} 처리 중 시간 초과", outbox.getId());
            }

            int updated = outboxRepository.updateRecoverIfProcessing(
                    outbox.getId(),
                    outboxStatus,
                    errorMessage,
                    nextRetryAt
            );

            if (updated == 0) {
                log.info("Outbox {} 복구 건너뜀 : 이미 다른 트랜잭션에서 상태 변경됨", outbox.getId());
            }
        }
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
}
