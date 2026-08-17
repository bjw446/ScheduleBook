package com.example.schedulebook.domain.deadletter.service;

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
            int retryCount
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
                retryCount
        ));
    }

    @Transactional
    public void markRecovered(Long deadLetterId) {
        if (deadLetterRepository.markRecovered(deadLetterId) != 1) {
            throw new BaseException(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(Long deadLetterId) {
        if (deadLetterRepository.markProcessing(deadLetterId) != 1) {
            throw new BaseException(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPending(Long deadLetterId) {
        if (deadLetterRepository.markPending(deadLetterId) != 1) {
            throw new BaseException(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);
        }
    }
}
