package com.example.schedulebook.domain.outbox.entity;

import com.example.schedulebook.common.entity.ModifyEntity;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "outbox",
        indexes = {
                @Index(name = "idx_outbox_status", columnList = "status"),
                @Index(name = "idx_outbox_retry", columnList = "next_retry_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox extends ModifyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false)
    private OutboxAggregateType aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(length = 500, name = "error_message")
    private String errorMessage;

    @Column(name = "processing_at")
    private LocalDateTime processingAt;

    public static Outbox create(OutboxAggregateType aggregateType, Long aggregateId, OutboxEventType eventType, String payload) {
        Outbox outbox = new Outbox();

        outbox.aggregateType = aggregateType;
        outbox.aggregateId = aggregateId;
        outbox.eventType = eventType;
        outbox.payload = payload;
        outbox.status = OutboxStatus.PENDING;
        outbox.retryCount = 0;
        outbox.nextRetryAt = LocalDateTime.now();

        return outbox;
    }

    public void processing() {
        if (this.status != OutboxStatus.PENDING && this.status != OutboxStatus.FAILED) {
            throw new BaseException(ErrorEnum.INVALID_OUTBOX_STATUS);
        }

        this.status = OutboxStatus.PROCESSING;
        this.processingAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }
}
