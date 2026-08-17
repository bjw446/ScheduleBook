package com.example.schedulebook.domain.admin.dto.response;

import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;

import java.time.LocalDateTime;

public record DeadLetterDetailResponse(
        Long deadLetterId,
        DeadLetterType deadLetterType,
        DeadLetterSource deadLetterSource,
        DeadLetterAggregateType deadLetterAggregateType,
        DeadLetterStatus deadLetterStatus,
        String aggregateId,
        Long userId,
        String payload,
        String exceptionType,
        String reason,
        int retryCount,
        LocalDateTime failedAt
) {
    public static DeadLetterDetailResponse from(DeadLetterQueue deadLetterQueue) {
        return new DeadLetterDetailResponse(
                deadLetterQueue.getId(),
                deadLetterQueue.getDeadLetterType(),
                deadLetterQueue.getDeadLetterSource(),
                deadLetterQueue.getDeadLetterAggregateType(),
                deadLetterQueue.getDeadLetterStatus(),
                deadLetterQueue.getAggregateId(),
                deadLetterQueue.getUserId(),
                deadLetterQueue.getPayload(),
                deadLetterQueue.getExceptionType(),
                deadLetterQueue.getReason(),
                deadLetterQueue.getRetryCount(),
                deadLetterQueue.getFailedAt()
        );
    }
}
