package com.example.schedulebook.domain.admin.dto.response;

import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;

import java.time.LocalDateTime;

public record DeadLetterSummaryResponse(
        Long deadLetterId,
        DeadLetterType deadLetterType,
        DeadLetterSource deadLetterSource,
        DeadLetterAggregateType deadLetterAggregateType,
        DeadLetterStatus deadLetterStatus,
        String aggregateId,
        LocalDateTime failedAt
) {
    public static DeadLetterSummaryResponse from(DeadLetterQueue deadLetterQueue) {
        return new DeadLetterSummaryResponse(
                deadLetterQueue.getId(),
                deadLetterQueue.getDeadLetterType(),
                deadLetterQueue.getDeadLetterSource(),
                deadLetterQueue.getDeadLetterAggregateType(),
                deadLetterQueue.getDeadLetterStatus(),
                deadLetterQueue.getAggregateId(),
                deadLetterQueue.getFailedAt()
        );
    }
}
