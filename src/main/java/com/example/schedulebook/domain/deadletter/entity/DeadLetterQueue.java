package com.example.schedulebook.domain.deadletter.entity;

import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "dead_letter_queues",
        indexes = {
                @Index(
                        name = "idx_dlq_status_processing_at",
                        columnList = "dead_letter_status, processing_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeadLetterQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dead_letter_type", nullable = false)
    private DeadLetterType deadLetterType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dead_letter_source", nullable = false)
    private DeadLetterSource deadLetterSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "dead_letter_aggregate_type")
    private DeadLetterAggregateType deadLetterAggregateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dead_letter_status", nullable = false)
    private DeadLetterStatus deadLetterStatus;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "exception_type")
    private String exceptionType;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @Column(name = "processing_at")
    private LocalDateTime processingAt;

    @Column(name = "claim_token")
    private String claimToken;

    @Column(name = "event_id", unique = true)
    private String eventId;

    public static DeadLetterQueue create(
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
        DeadLetterQueue deadLetterQueue = new DeadLetterQueue();

        deadLetterQueue.deadLetterType = deadLetterType;
        deadLetterQueue.deadLetterSource = deadLetterSource;
        deadLetterQueue.deadLetterAggregateType = deadLetterAggregateType;
        deadLetterQueue.deadLetterStatus = DeadLetterStatus.PENDING;
        deadLetterQueue.aggregateId = aggregateId;
        deadLetterQueue.userId = userId;
        deadLetterQueue.payload = payload;
        deadLetterQueue.reason = reason;
        deadLetterQueue.exceptionType = exceptionType;
        deadLetterQueue.retryCount = retryCount;
        deadLetterQueue.failedAt = LocalDateTime.now();
        deadLetterQueue.eventId = eventId;

        return deadLetterQueue;
    }
}
