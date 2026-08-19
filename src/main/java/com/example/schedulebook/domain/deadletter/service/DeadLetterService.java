package com.example.schedulebook.domain.deadletter.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.repository.DeadLetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeadLetterService {
    private final DeadLetterRepository deadLetterRepository;

    @Transactional
    public void save(
            DeadLetterType deadLetterType,
            DeadLetterSource deadLetterSource,
            DeadLetterAggregateType deadLetterAggregateType,
            String aggregateId,
            Long userId,
            String payload,
            String reason,
            String exceptionType,
            int retryCount,
            String eventId
    ) {
        deadLetterRepository.save(DeadLetterQueue.create(
                deadLetterType,
                deadLetterSource,
                deadLetterAggregateType,
                aggregateId,
                userId,
                payload,
                reason,
                exceptionType,
                retryCount,
                eventId
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRecovered(Long deadLetterId, String claimToken) {
        if (deadLetterRepository.markRecovered(deadLetterId, claimToken) != 1) {
            throw new BaseException(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String markProcessing(Long deadLetterId) {
        String claimToken = UUID.randomUUID().toString();

        LocalDateTime leaseUntil = LocalDateTime.now().plusMinutes(CommonConst.DEAD_LETTER_PROCESSING_LEASE_MINUTES);

        if (deadLetterRepository.markProcessing(deadLetterId, claimToken, leaseUntil) != 1) {
            throw new BaseException(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);
        }

        return claimToken;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPending(Long deadLetterId, String claimToken) {
        if (deadLetterRepository.markPending(deadLetterId, claimToken) != 1) {
            throw new BaseException(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int reclaimExpiredProcessing() {
        return deadLetterRepository.reclaimExpiredProcessing();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void renewLease(Long deadLetterId, String claimToken) {
        LocalDateTime leaseUntil = LocalDateTime.now().plusMinutes(CommonConst.DEAD_LETTER_PROCESSING_LEASE_MINUTES);

        if (deadLetterRepository.renewLease(deadLetterId, claimToken, leaseUntil) != 1) {
            throw new BaseException(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);
        }
    }
}
