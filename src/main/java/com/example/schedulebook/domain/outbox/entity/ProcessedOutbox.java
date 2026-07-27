package com.example.schedulebook.domain.outbox.entity;

import com.example.schedulebook.common.entity.ModifyEntity;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Entity
@Table(
        name = "processed_outbox",
        uniqueConstraints = @UniqueConstraint(
                columnNames = "outbox_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedOutbox extends ModifyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbox_id", nullable = false)
    private Long outboxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false)
    private OutboxAggregateType aggregateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processed_outbox_status", nullable = false)
    private ProcessedOutboxStatus status;

    public static ProcessedOutbox of(
            Long outboxId,
            OutboxAggregateType aggregateType,
            OutboxEventType eventType
    ) {
        ProcessedOutbox processedOutbox = new ProcessedOutbox();

        processedOutbox.outboxId = outboxId;
        processedOutbox.aggregateType = aggregateType;
        processedOutbox.eventType = eventType;
        processedOutbox.status = ProcessedOutboxStatus.PROCESSING;

        return processedOutbox;
    }

    public void success() {
        if (this.status != ProcessedOutboxStatus.PROCESSING) {
            throw new BaseException(ErrorEnum.INVALID_PROCESSED_OUTBOX_STAUS);
        }

        this.status = ProcessedOutboxStatus.SUCCESS;
    }

    public void fail() {
        if (this.status != ProcessedOutboxStatus.PROCESSING) {
            throw new BaseException(ErrorEnum.INVALID_PROCESSED_OUTBOX_STAUS);
        }

        this.status = ProcessedOutboxStatus.FAILED;
    }

    public void retry() {
        if (this.status != ProcessedOutboxStatus.FAILED) {
            throw new BaseException(ErrorEnum.INVALID_PROCESSED_OUTBOX_STAUS);
        }

        this.status = ProcessedOutboxStatus.PROCESSING;
    }
}
