package com.example.schedulebook.domain.outbox.entity;

import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "processed_outbox",
        uniqueConstraints = @UniqueConstraint(
                columnNames = "outbox_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedOutbox {
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

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static ProcessedOutbox of(
            Long outboxId,
            OutboxAggregateType aggregateType,
            OutboxEventType eventType,
            LocalDateTime processedAt
    ) {
        ProcessedOutbox processedOutbox = new ProcessedOutbox();

        processedOutbox.outboxId = outboxId;
        processedOutbox.aggregateType = aggregateType;
        processedOutbox.eventType = eventType;
        processedOutbox.processedAt = processedAt;

        return processedOutbox;
    }
}
