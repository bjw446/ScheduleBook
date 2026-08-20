package com.example.schedulebook.domain.admin.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.response.PageResponse;
import com.example.schedulebook.domain.admin.dto.response.DeadLetterDetailResponse;
import com.example.schedulebook.domain.admin.dto.response.DeadLetterSummaryResponse;
import com.example.schedulebook.domain.auth.service.ForceLogoutRetryService;
import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus;
import com.example.schedulebook.domain.deadletter.repository.DeadLetterRepository;
import com.example.schedulebook.domain.deadletter.service.DeadLetterDeserializationRecoveryService;
import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.outbox.service.OutboxTransactionService;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDeadLetterService {
    private final DeadLetterRepository deadLetterRepository;
    private final UserValidator userValidator;
    private final ForceLogoutRetryService forceLogoutRetryService;
    private final OutboxTransactionService outboxTransactionService;
    private final DeadLetterService deadLetterService;
    private final DeadLetterDeserializationRecoveryService deadLetterDeserializationRecoveryService;
    private final NotificationRetryService notificationRetryService;

    @Transactional(readOnly = true)
    public PageResponse<DeadLetterSummaryResponse> findAllDeadLetters(Long currentUserId, Pageable pageable) {
        userValidator.validateActiveAdmin(currentUserId);

        Page<DeadLetterQueue> deadLetterQueuePage = deadLetterRepository.findAll(pageable);

        return PageResponse.register(deadLetterQueuePage.map(DeadLetterSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public DeadLetterDetailResponse findOneDeadLetter(Long currentUserId, Long deadLetterId) {
        userValidator.validateActiveAdmin(currentUserId);

        DeadLetterQueue deadLetterQueue = deadLetterRepository.findById(deadLetterId).orElseThrow(
                () -> new BaseException(ErrorEnum.DEAD_LETTER_NOT_FOUND)
        );

        return DeadLetterDetailResponse.from(deadLetterQueue);
    }

    public void recoverDeadLetter(Long currentUserId, Long deadLetterId) {
        userValidator.validateActiveAdmin(currentUserId);

        String claimToken = deadLetterService.markProcessing(deadLetterId);

        DeadLetterQueue deadLetterQueue = deadLetterRepository.findByIdAndClaimTokenAndDeadLetterStatus(
                deadLetterId,
                claimToken,
                DeadLetterStatus.PROCESSING
        ).orElseThrow(() -> new BaseException(ErrorEnum.DEAD_LETTER_CLAIM_FAILED));

        try {
            recover(deadLetterQueue);

            deadLetterService.markRecovered(
                    deadLetterId,
                    claimToken
            );

        } catch (Exception e) {
            try {
                deadLetterService.markPending(
                        deadLetterId,
                        claimToken
                );

            } catch (Exception recoveryException) {
                log.error(
                        "DeadLetter 상태 복구 자체 실패 deadLetterId = {}, originalError={}",
                        deadLetterId,
                        e.getMessage(),
                        recoveryException
                );
            }

            throw e;
        }
    }

    private void recover(DeadLetterQueue deadLetterQueue) {
        switch (deadLetterQueue.getDeadLetterAggregateType()) {
            case SESSION ->
                    forceLogoutRetryService.recover(deadLetterQueue.getAggregateId());

            case OUTBOX ->
                    outboxTransactionService.recover(Long.valueOf(deadLetterQueue.getAggregateId()));

            case NOTIFICATION_RETRY ->
                notificationRetryService.recover(Long.valueOf(deadLetterQueue.getAggregateId()));

            case DESERIALIZATION_ERROR ->
                    deadLetterDeserializationRecoveryService.recover(deadLetterQueue);

            default -> throw new BaseException(ErrorEnum.INVALID_DEAD_LETTER_TYPE);
        }
    }
}
