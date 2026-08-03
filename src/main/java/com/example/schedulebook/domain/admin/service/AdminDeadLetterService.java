package com.example.schedulebook.domain.admin.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.response.PageResponse;
import com.example.schedulebook.domain.admin.dto.response.DeadLetterDetailResponse;
import com.example.schedulebook.domain.admin.dto.response.DeadLetterSummaryResponse;
import com.example.schedulebook.domain.auth.service.ForceLogoutRetryService;
import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.repository.DeadLetterRepository;
import com.example.schedulebook.domain.outbox.service.OutboxTransactionService;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AdminDeadLetterService {
    private final DeadLetterRepository deadLetterRepository;
    private final UserValidator userValidator;
    private final ForceLogoutRetryService forceLogoutRetryService;
    private final OutboxTransactionService outboxTransactionService;

    @Transactional(readOnly = true)
    public PageResponse<DeadLetterSummaryResponse> findAllDeadLetters(Long currentUserId, Pageable pageable) {
        userValidator.validateActiveAdmin(currentUserId);

        Page<DeadLetterQueue> deadLetterQueuePage = deadLetterRepository.findAllPages(pageable);

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

    @Transactional
    public void recoverDeadLetter(Long currentUserId, Long deadLetterId) {
        userValidator.validateActiveAdmin(currentUserId);

        DeadLetterQueue deadLetterQueue = deadLetterRepository.findById(deadLetterId).orElseThrow(
                () -> new BaseException(ErrorEnum.DEAD_LETTER_NOT_FOUND)
        );

        switch (deadLetterQueue.getDeadLetterAggregateType()) {
            case SESSION ->
                    forceLogoutRetryService.recover(Long.valueOf(deadLetterQueue.getAggregateId()));

            case OUTBOX ->
                    outboxTransactionService.recover(Long.valueOf(deadLetterQueue.getAggregateId()));

            default -> throw new BaseException(ErrorEnum.INVALID_DEAD_LETTER_AGGREGATE_TYPE);
        }
    }
}
