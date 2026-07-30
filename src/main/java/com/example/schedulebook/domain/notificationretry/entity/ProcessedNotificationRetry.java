package com.example.schedulebook.domain.notificationretry.entity;

import com.example.schedulebook.common.entity.ModifyEntity;
import com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "processed_notification_retry",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "outbox_id",
                        "receiver_id"
                }
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedNotificationRetry extends ModifyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="outbox_id", nullable=false)
    private Long outboxId;

    @Column(name="receiver_id", nullable=false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private ProcessedNotificationRetryStatus status;

    @Column(name = "processing_owner", length = 36)
    private String processingOwner;

    public static ProcessedNotificationRetry create(Long outboxId, Long receiverId, String processingOwner) {
        ProcessedNotificationRetry processedNotificationRetry = new ProcessedNotificationRetry();

        processedNotificationRetry.outboxId = outboxId;
        processedNotificationRetry.receiverId = receiverId;
        processedNotificationRetry.status = ProcessedNotificationRetryStatus.PROCESSING;
        processedNotificationRetry.processingOwner = processingOwner;

        return processedNotificationRetry;
    }
}
