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
        Outbox outbox = outboxRepository.findByIdAndStatus(outboxId, OutboxStatus.PROCESSING)
                .orElseThrow(() -> new BaseException(ErrorEnum.OUTBOX_NOT_FOUND));

        outbox.success();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(Long outboxId, Exception e) {
        Outbox outbox = outboxRepository.findByIdAndStatus(outboxId, OutboxStatus.PROCESSING)
                        .orElseThrow(() -> new BaseException(ErrorEnum.OUTBOX_NOT_FOUND));

        outbox.increaseRetryCount();

        if (outbox.getRetryCount() >= CommonConst.MAX_RETRY) {
            outbox.dead(normalizeMessage(e));

            log.error("Outbox {} 영구 실패", outbox.getId(), e);

        } else {
            outbox.fail(normalizeMessage(e), LocalDateTime.now().plusMinutes(CommonConst.OUTBOX_RETRY_DELAY));

            log.error("Outbox 발행 실패 : {}", outbox.getId(), e);
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

            if (outbox.getRetryCount() >= CommonConst.MAX_RETRY) {
                outbox.dead("Outbox lease timeout으로 DEAD 처리");

                log.error("Outbox {} lease timeout으로 DEAD 처리", outbox.getId());

            } else {
                outbox.fail(
                        "Outbox 처리 중 시간 초과",
                        LocalDateTime.now().plusMinutes(CommonConst.OUTBOX_RETRY_DELAY)
                );
            }
        }

        outboxRepository.flush();
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
